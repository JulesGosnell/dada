(ns
 org.dada.core
 (:require [org.dada.core SessionManagerImpl SimpleModelView])
 (:use [clojure.contrib logging])
 (:import
  (clojure.lang
   DynamicClassLoader
   ISeq
   Keyword
   )
  (java.sql
   Connection
   ResultSet
   ResultSetMetaData
   )
  (java.util
   Collection
   Date)
  (java.util.concurrent.locks
   Lock)
  (org.springframework.context.support ClassPathXmlApplicationContext)
  (org.springframework.beans.factory BeanFactory)
  (org.dada.asm ClassFactory)
  (org.dada.core
   AbstractModel
   Attribute
   Creator
   DummyLock
   Getter
   Metadata
   Metadata$VersionComparator
   MetadataImpl
   Model
   ServiceFactory
   SessionManager
   SessionManagerImpl
   StringMetadata
   SynchronousServiceFactory
   SimpleModelView
   Update
   View
   )
  (org.dada.demo Client)
  ))

;;--------------------------------------------------------------------------------

(set! *warn-on-reflection* true)

;; TODO: Spring should look after this - see application-context.xml...
(if (not (System/getProperty "dada.broker.name")) (System/setProperty "dada.broker.name" "DADA"))
(if (not (System/getProperty "dada.broker.uri")) (System/setProperty "dada.broker.uri" "tcp://0.0.0.0:61616"))
(if (not (System/getProperty "dada.client.uri")) (System/setProperty "dada.client.uri" "tcp://localhost:61616"))

(def *dada-broker-name* (System/getProperty "dada.broker.name"))
(def *session-manager-name* "SessionManager")

(defn insert [#^View view item]
  (.update view (list (Update. nil item)) '() '())
  item)

(defn insert-n [#^View view #^ISeq items]
  (.update view (map (fn [item] (Update. nil item)) items) '() '()))

(defn update [#^View view oldValue newValue]
  (.update view '() (list (Update. oldValue newValue)) '()))

(defn delete [#^View view value]
  (.update view '() '() (list (Update. value nil))))

;;--------------------------------------------------------------------------------

;; TODO: should we register before reading data or vice versa... - can we do this without a lock ?
(defn connect [#^Model model #^View view]
  (let [[extant extinct] (.registerView model view)
	additions (map #(Update. nil %) extant)
	subtractions (map #(Update. nil %) extinct)]
    (if (or (not (empty? additions)) (not (empty? subtractions)))
      (.update view additions '() subtractions)))
  view)

(defn disconnect [#^Model model #^View view]
  (let [[extant extinct] (.registerView model view)
	additions (map #(Update. nil %) extant)
	subtractions (map #(Update. nil %) extinct)]
    (if (or (not (empty? additions)) (not (empty? subtractions)))
      (.update view '() '() (concat additions subtractions))))
  view)

(defmulti attribute-key (fn [arg] (class arg)))
(defmethod attribute-key Date [#^Date date] (attribute-key (str "attribute" date)))
(defmethod attribute-key Integer [#^Integer i] (attribute-key (str "attribute" i)))
(defmethod attribute-key clojure.lang.Keyword [#^Keyword keyword] (attribute-key (name keyword)))

(def string-replacements
     (array-map
     "_" "_underscore_"
     " " "_space_"
     ":" "_colon_"
     "." "_fullstop_"
     "*" "_asterisk_"
     "+" "_plus_"
     "-" "_minus_"
     "/" "_divide_"
     "(" "_openroundbracket_"
     ")" "_closeroundbracket_"
     ))

(defmethod attribute-key String [#^String string] 
  (reduce (fn [#^String string [#^String key #^String value]] (.replace string key value))
	  string
	  string-replacements))

;; see http://groups.google.com/group/clojure/browse_thread/thread/106a2f73fb49f492#

;; I'm going with directly invoking this ctor, because it seems to be
;; calling a ctor directly on a class, although the other option calls
;; new on a symbol... What I really want to do is come up with
;; something that compiles down to bytecode that calls the correct
;; ctor for the given args directly... - TODO - more thought...
(defn make-instance [#^Class class & args]
  (clojure.lang.Reflector/invokeConstructor class (to-array args)))

;;--------------------------------------------------------------------------------
;; handle custom engineered classes

(def #^ClassFactory *custom-class-factory* (new ClassFactory))

(def *custom-classloader* (deref clojure.lang.Compiler/LOADER))

(defn #^DynamicClassLoader custom-classloader []
  (try
   (deref clojure.lang.Compiler/LOADER)
   (catch IllegalStateException _ (warn "could not retrieve ClassLoader") *custom-classloader*)))

;; e.g.
;;(make-class
;; factory
;; "org.dada.tmp.Amount"
;; :id int :version int :amount double)

(defn custom-attribute-array [& attribute-key-types]
  (if (empty? attribute-key-types)
    nil
    (into-array (map 
		 (fn [[key #^Class type]]
		     (into-array (list (.getCanonicalName type) (attribute-key key))))
		 (apply array-map attribute-key-types)))))

;;(def *exported-classes* (atom {}))

(defn custom-class [#^String class-name #^Class superclass #^ISeq & attribute-key-types]
  (let [bytes (.create
	       *custom-class-factory*
	       class-name
	       superclass
	       (apply custom-attribute-array attribute-key-types))]
    ;;(swap! *exported-classes*  (fn [classes] (assoc classes class-name bytes)))
    (.
     (custom-classloader)
     (defineClass class-name bytes :TODO)
     )))
  
;; fields => [model field]


;; TODO: how do we confirm that with-meta is doing the right thing for
;; both input and output types...

(defn custom-getter-name [#^String property-name]
  (str "get" (.toUpperCase (.substring property-name 0 1)) (.substring property-name 1 (.length property-name))))

(defn custom-getter2 [#^Class input-type #^Class output-type #^String method-name]
  (let [method-symbol (symbol (str "." method-name))
	arg-symbol (with-meta 's {:tag (.getCanonicalName input-type)})]
    (eval `(proxy [Getter] [] 
		  (#^{:tag ~output-type} get [~arg-symbol] (~method-symbol ~arg-symbol))))))

(defn custom-getter [#^Class input-type #^Class output-type key]
  "return a Getter taking input-type returning output-type and calling get<Key>"
  (custom-getter2 input-type output-type (custom-getter-name (attribute-key key))))

(defn custom-creator [#^Class class]
  "make a Creator for the given Class"
  (proxy [Creator] [] 
	 (#^{:tag class} create [#^{:tag (type (into-array Object []))} args]
		  ;;(println "CREATOR" class (map identity args))
		  (apply make-instance class args))))

(defn #^Metadata custom-metadata3 [#^Class class primary-keys version-keys version-comparator attribute-specs]
  "make Metadata for a given class"
  (new MetadataImpl
       (custom-creator class)
       primary-keys
       version-keys
       version-comparator
       (map
	(fn [[key type mutable]] (Attribute. key type mutable (custom-getter class type key)))
	attribute-specs)))

(defn #^Metadata custom-metadata2
  "create metadata for a Model containing instances of a Class"
  [#^String class-name #^Class superclass #^Collection primary-keys #^Collection version-keys #^Metadata$VersionComparator version-comparator #^Collection attributes]
  (let [class-attributes (mapcat (fn [[key type _]] [key type]) attributes)]
    (custom-metadata3 (apply custom-class class-name superclass class-attributes) primary-keys version-keys version-comparator attributes)))

(let [custom-metadata-cache (atom {})]

  (defn #^Metadata custom-metadata
    "create metadata for a Model containing instances of a Class"
    [#^String class-name #^Class superclass #^Collection primary-keys #^Collection version-keys #^Metadata$VersionComparator version-comparator #^Collection attributes]
    (let [cache-key [superclass primary-keys version-keys attributes]]
      ((swap!
	custom-metadata-cache 
	(fn [cache key]
	    (if (contains? cache key)
	      cache
	      (assoc cache key (custom-metadata2 class-name superclass primary-keys version-keys version-comparator attributes))))
	cache-key)
       cache-key)))

  )

;;--------------------------------------------------------------------------------
;; handle sequences

(defn #^Metadata seq-metadata [length]
  (new MetadataImpl
       (proxy [Creator] [] (create [args] args))
       [0]
       [1]
       (proxy [Metadata$VersionComparator][] (compareTo [[_ v1] [_ v2]] (- v1 v2)))
       (map
	(fn [i] (Attribute. i Object (= i 0) (proxy [Getter] [] (get [s] (nth s i)))))
	(range length))))


;;--------------------------------------------------------------------------------
;; handle records

(defn #^Class record-class [attributes]
  "make an anonymous record Class"
  (let [sym (gensym "Record")
	keys (map (fn [[key type]] (symbol (attribute-key key))) attributes)]
    (eval `(do (defrecord ~sym ~keys) ~sym)))) ; TODO: put type hints on fields

(defn #^Class named-record-class [class-name attributes]
  "make an anonymous record Class"
  (let [sym (symbol class-name)
	keys (map (fn [[key type]] (symbol (attribute-key key))) attributes)]
    (eval `(do (defrecord ~sym ~keys) ~sym))))

(defn #^Creator record-creator [#^Class class]
  "make a Creator for the given Class"
  (proxy [Creator] [] 
	 (#^{:tag class} create [#^{:tag (type (into-array Object []))} args] ;TODO: why does creator insist on an array ?
		  (apply make-instance class args))))

(defn #^Getter record-getter [#^Class input-type #^Class output-type name]
  "make a Getter for the named attribute of the given Class"
  (let [key (keyword (attribute-key name))
	arg-symbol (with-meta 's {:tag (.getCanonicalName input-type)})]
    (eval `(proxy [Getter] [] 
		  (#^{:tag ~output-type} get [~arg-symbol] (~key ~arg-symbol))))))


(defn #^Metadata record-metadata2 [primary-keys version-keys version-comparator attributes]
  "make a record-based Metadata instance"
  (let [class (record-class attributes)]
    (new MetadataImpl
	 (record-creator class)
	 primary-keys
	 version-keys
	 version-comparator
	 (map (fn [[key type mutable]] (Attribute. key type mutable (record-getter class type key))) attributes))))

(defn #^Metadata named-record-metadata2 [class-name primary-keys version-keys version-comparator attributes]
  "make a record-based Metadata instance"
  (let [class (named-record-class class-name attributes)]
    (new MetadataImpl
	 (record-creator class)
	 primary-keys
	 version-keys
	 version-comparator
	 (map (fn [[key type mutable]] (Attribute. key type mutable (record-getter class type key))) attributes))))

(let [record-metadata-cache (atom {})]

  (defn #^Metadata record-metadata
    "return memoized record-metadata"
    [primary-keys version-keys version-comparator attributes]
    (let [cache-key [primary-keys version-keys version-comparator attributes]]
      ((swap!
	record-metadata-cache 
	(fn [cache key]
	    (if (contains? cache key) cache (assoc cache key (record-metadata2 primary-keys version-keys version-comparator attributes))))
	cache-key)
       cache-key)))

  (defn #^Metadata named-record-metadata
    "return memoized record-metadata"
    [class-name primary-keys version-keys version-comparator attributes]
    (let [cache-key [primary-keys version-keys version-comparator attributes]]
      ((swap!
	record-metadata-cache 
	(fn [cache key]
	    (if (contains? cache key) cache (assoc cache key (named-record-metadata2 class-name primary-keys version-keys version-comparator attributes))))
	cache-key)
       cache-key)))

  )

(defmacro defcreator [class-name field-names]
  `(proxy [Creator] []
     (^{:tag ~class-name} create [#^{:tag (type (into-array Object []))} args#]
      (apply (fn [~@field-names] (~(symbol (str (name class-name) "." )) ~@field-names)) args#))))

;; TODO - split out  defgetter and defattribute
(defmacro def-record-metadata [var-name class-name fields & [version-comparator]]
  `(do
     (defrecord ~class-name ~fields)
     (def ^Metadata ~var-name
	  (MetadataImpl.
	   (defcreator ~class-name ~fields)
	   (list ~@(map keyword (filter (fn [field] (:primary-key (meta field))) fields)))
	   (list ~@(map keyword (filter (fn [field] (:version-key (meta field))) fields)))
	   (proxy [Metadata$VersionComparator] [] (compareTo [lhs# rhs#] (~version-comparator lhs# rhs#)))
	   (list
	    ~@(map 
	       (fn [field]
		 (let [m (meta field)
		       key (keyword field)
		       tag (or (:tag m) Object)]
		   `(Attribute.
		     ~key
		     ~tag
		     ~(not (or (:primary-key m) (:immutable m)))
		     (proxy [Getter] [] (^{:tag ~tag} get [^{:tag ~class-name} datum#] (~(symbol (str "." (name key))) datum#))))))
	       fields))
	   ))
     ))

;;--------------------------------------------------------------------------------

(defn sql-attributes 
  "Derive a sequence of MetaData attributes from an SQL ResultSet"
  [#^ResultSetMetaData sql-metadata type-translations]
  (map
   (fn [column]
       (let [key (keyword (.getColumnName sql-metadata column))]
	 [key
	  (or (let [[type] (type-translations key)] type)
	      (Class/forName (.getColumnClassName sql-metadata column)))
	  true]))		 ;we don't know - so assume mutability
   (range 1 (+ 1 (.getColumnCount sql-metadata)))))

(defn sql-data
  "Create a sequence from some MetaData and a SQL ResultSet"
  [#^Metadata metadata #^ResultSet result-set type-translations]
  (let [creator (.getCreator metadata)
	readers (map
		 (fn [#^Attribute attribute]
		     (let [[_ translator] (type-translations (.getKey attribute))]
		       (or translator
			   (fn [#^ResultSet result-set #^Integer column-index] (.getObject result-set column-index)))))
		 (.getAttributes metadata))]
    (loop [output nil]
      (if (.next result-set)
	(recur
	 (cons
	  (.create 
	   creator 
	   (into-array
	    Object
	    (map 
	     (fn [reader #^Integer column-index] (reader result-set column-index))
	     readers
	     (iterate inc 1))))
	  output))
	output))))

;;--------------------------------------------------------------------------------

(defn model [#^String model-name #^Metadata metadata]
  (SimpleModelView. model-name metadata))

;; this should really be collapsed into (model) above - but arity overloading is not sufficient...
(defn clone-model [#^Model model #^String name]
  (SimpleModelView. name (.getMetadata model)))

(defn sql-model
  "make a SQL query and return the ResultSet as a Model"
  [model-name #^Connection connection #^String sql primary-keys version-keys version-comparator & [type-translations]]
  (let [result-set (.executeQuery (.prepareStatement connection sql))
	metadata (custom-metadata 
		  (name (gensym "org.dada.core.SQLModel"))
		  Object
		  primary-keys
		  version-keys
		  version-comparator
		  (sql-attributes (.getMetaData result-set) type-translations))
	data (sql-data metadata result-set type-translations)
	sql-model (model model-name metadata)]
    (.close result-set)
    (insert-n sql-model data)
    sql-model))

;;--------------------------------------------------------------------------------

(def metamodel-metadata (MetadataImpl.
			 nil		;creator
			 [:name]	;primary-keys
			 []		;version-keys
			 (proxy [Metadata$VersionComparator][](compareTo [old new] -1)) ;version-comparator
			 [(Attribute. :name String false (proxy [Getter][] (get [#^Model model] (.getName model))))])) ;attributes

(do
  (def #^ServiceFactory *external-session-manager-service-factory* (SynchronousServiceFactory.))
  (def #^ServiceFactory *external-view-service-factory* (SynchronousServiceFactory.))
  (def #^ServiceFactory *internal-view-service-factory* (SynchronousServiceFactory.))
  (def #^Lock *exclusive-lock* (DummyLock.))
  (def #^Model *metamodel* (SimpleModelView. "MetaModel" metamodel-metadata))
  (insert *metamodel* *metamodel*)
  (def #^SessionManager *session-manager* (SessionManagerImpl. *session-manager-name* *metamodel* *external-view-service-factory*)))

(defn start-server []

  (do
    (def #^ClassPathXmlApplicationContext *spring-context* (ClassPathXmlApplicationContext. "application-context.xml"))
    (def #^ServiceFactory *external-session-manager-service-factory* (.getBean #^BeanFactory *spring-context* "externalSessionManagerServiceFactory"))
    (def #^ServiceFactory *external-view-service-factory* (.getBean #^BeanFactory *spring-context* "externalViewServiceFactory"))
    (def #^ServiceFactory *internal-view-service-factory* (.getBean #^BeanFactory *spring-context* "internalViewServiceFactory"))
    (def #^Lock *exclusive-lock* (.getBean #^BeanFactory *spring-context* "writeLock"))

    (def #^Model *metamodel* (SimpleModelView. "MetaModel" metamodel-metadata))
    (insert *metamodel* *metamodel*)
    (def #^SessionManager *session-manager* (SessionManagerImpl. *session-manager-name* *metamodel* *external-view-service-factory*))
    )

  ;;(.start *metamodel*)
    (info (str "Server: " *session-manager-name*))
    (.server *external-session-manager-service-factory* *session-manager* *session-manager-name*))

(defn start-client []
  (Client/main (into-array String (list *dada-broker-name*))))

;;--------------------------------------------------------------------------------
