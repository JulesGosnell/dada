(ns org.dada.core
 (:require [org.dada.core SimpleModelView])
 (:use [clojure.tools logging])
 (:use [org.dada.core utils])
 (:import
  (clojure.lang
   DynamicClassLoader
   ISeq
   Keyword
   )
  [java.io
   ByteArrayInputStream
   ByteArrayOutputStream
   ObjectOutputStream]
  (java.sql
   Connection
   ResultSet
   ResultSetMetaData
   )
  (java.util
   Collection
   Date
   HashMap)
  (java.util.concurrent.locks
   Lock)
  (org.springframework.context.support ClassPathXmlApplicationContext)
  (org.springframework.beans.factory BeanFactory)
  (org.dada.asm ClassFactory)
  (org.dada.core
   AbstractModel
   Attribute
   ClassLoadingAwareObjectInputStream
   Creator
   DummyLock
   Getter
   Metadata
   Metadata$VersionComparator
   MetadataImpl
   Model
   ServiceFactory
   StringMetadata
   SynchronousServiceFactory
   SimpleModelView
   Update
   View
   )
  ))

;;--------------------------------------------------------------------------------

(set! *warn-on-reflection* true)

;; TODO: Spring should look after this - see application-context.xml...
(if (not (System/getProperty "dada.broker.name")) (System/setProperty "dada.broker.name" "DADA"))
(if (not (System/getProperty "dada.broker.uri")) (System/setProperty "dada.broker.uri" "tcp://0.0.0.0:61616"))
(if (not (System/getProperty "dada.client.uri")) (System/setProperty "dada.client.uri" "tcp://localhost:61616"))

(defn insert [^View view item]
  (.update view (list (Update. nil item)) '() '())
  item)

(defn insert-n [^View view ^ISeq items]
  (.update view (map (fn [item] (Update. nil item)) items) '() '()))

(defn update [^View view oldValue newValue]
  (.update view '() (list (Update. oldValue newValue)) '()))

(defn delete [^View view value]
  (.update view '() '() (list (Update. value nil))))

(defn delete-n [^View view ^ISeq items]
  (.update view '() '() (map (fn [item] (Update. item nil)) items)))

;;--------------------------------------------------------------------------------

;; TODO: should we register before reading data or vice versa... - can we do this without a lock ?
(defn connect [^Model model ^View view]
  (let [[extant extinct] (.attach model view)
	additions (map #(Update. nil %) extant)
	subtractions (map #(Update. nil %) extinct)]
    (if (or (not (empty? additions)) (not (empty? subtractions)))
      (.update view additions '() subtractions)))
  view)

(defn disconnect [^Model model ^View view]
  (let [[extant extinct] (.attach model view)
	additions (map #(Update. nil %) extant)
	subtractions (map #(Update. nil %) extinct)]
    (if (or (not (empty? additions)) (not (empty? subtractions)))
      (.update view '() '() (concat additions subtractions))))
  view)

(defmulti attribute-key (fn [arg] (class arg)))
(defmethod attribute-key Date [^Date date] (attribute-key (str "attribute" date)))
(defmethod attribute-key Integer [^Integer i] (attribute-key (str "attribute" i)))
(defmethod attribute-key clojure.lang.Keyword [^Keyword keyword] (attribute-key (name keyword)))

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

(defmethod attribute-key String [^String string] 
  (reduce (fn [^String string [^String key ^String value]] (.replace string key value))
	  string
	  string-replacements))

;; see http://groups.google.com/group/clojure/browse_thread/thread/106a2f73fb49f492#

;; I'm going with directly invoking this ctor, because it seems to be
;; calling a ctor directly on a class, although the other option calls
;; new on a symbol... What I really want to do is come up with
;; something that compiles down to bytecode that calls the correct
;; ctor for the given args directly... - TODO - more thought...
(defn make-instance [^Class class & args]
  (clojure.lang.Reflector/invokeConstructor class (to-array args)))

;;--------------------------------------------------------------------------------
;; handle custom engineered classes

(defn ^DynamicClassLoader custom-classloader []
  (try
    (deref clojure.lang.Compiler/LOADER)
    (catch IllegalStateException _ (warn "could not retrieve ClassLoader") (deref clojure.lang.Compiler/LOADER))))

;; e.g.
;;(make-class
;; factory
;; "org.dada.tmp.Amount"
;; :id int :version int :amount double)

(defn custom-attribute-array [& attribute-key-types]
  (if (empty? attribute-key-types)
    nil
    (into-array (map 
		 (fn [[key ^Class type]]
		     (into-array (list (.getCanonicalName type) (attribute-key key))))
		 (apply array-map attribute-key-types)))))

;;(def *exported-classes* (atom {}))

(let [^ClassFactory custom-class-factory (new ClassFactory)]
  (defn custom-class [^String class-name ^Class superclass ^ISeq & attribute-key-types]
    (let [bytes (.create
		 custom-class-factory
		 class-name
		 superclass
		 (apply custom-attribute-array attribute-key-types))]
      ;;(swap! *exported-classes*  (fn [classes] (assoc classes class-name bytes)))
      (.
       (custom-classloader)
       (defineClass class-name bytes :TODO)
       ))))
  
;; fields => [model field]


;; TODO: how do we confirm that with-meta is doing the right thing for
;; both input and output types...

(defn custom-getter-name [^String property-name]
  (str "get" (.toUpperCase (.substring property-name 0 1)) (.substring property-name 1 (.length property-name))))

(defn custom-getter2 [^Class input-type ^Class output-type ^String method-name]
  (let [method-symbol (symbol (str "." method-name))
	arg-symbol (with-meta 's {:tag (.getCanonicalName input-type)})]
    (eval `(proxy [Getter] [] 
		  (^{:tag ~output-type} get [~arg-symbol] (~method-symbol ~arg-symbol))))))

(defn custom-getter [^Class input-type ^Class output-type key]
  "return a Getter taking input-type returning output-type and calling get<Key>"
  (custom-getter2 input-type output-type (custom-getter-name (attribute-key key))))

(defn custom-creator [^Class class]
  "make a Creator for the given Class"
  (reify Creator (create [_ args] (apply make-instance class args))))

(defn ^Metadata custom-metadata3 [^Class class primary-keys version-keys version-comparator attribute-specs]
  "make Metadata for a given class"
  (new MetadataImpl
       (custom-creator class)
       primary-keys
       nil
       version-keys
       version-comparator
       (map
	(fn [[key type mutable]] (Attribute. key type mutable (custom-getter class type key)))
	attribute-specs)))

(defn ^Metadata custom-metadata2
  "create metadata for a Model containing instances of a Class"
  [^String class-name ^Class superclass ^Collection primary-keys ^Collection version-keys ^Metadata$VersionComparator version-comparator ^Collection attributes]
  (let [class-attributes (mapcat (fn [[key type _]] [key type]) attributes)]
    (custom-metadata3 (apply custom-class class-name superclass class-attributes) primary-keys version-keys version-comparator attributes)))

(let [custom-metadata-cache (atom {})]

  (defn ^Metadata custom-metadata
    "create metadata for a Model containing instances of a Class"
    [^String class-name ^Class superclass ^Collection primary-keys ^Collection version-keys ^Metadata$VersionComparator version-comparator ^Collection attributes]
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

(defn ^Metadata seq-metadata [length]
  (new MetadataImpl
       (reify Creator (create [_ args] args))
       [0]
       nil
       [1]
       (reify Metadata$VersionComparator (compareTo [_ [_ v1] [_ v2]] (- v1 v2)))
       (map
	(fn [i] (Attribute. i Object (= i 0) (reify Getter (get [_ s] (nth s i)))))
	(range length))))


;;--------------------------------------------------------------------------------
;; handle records

(defn ^Class record-class [attributes]
  "make an anonymous record Class"
  (let [sym (gensym "Record")
	keys (map (fn [[key type]] (symbol (attribute-key key))) attributes)]
    (eval `(do (defrecord ~sym ~keys) ~sym)))) ; TODO: put type hints on fields

(defn ^Class named-record-class [class-name attributes]
  "make an anonymous record Class"
  (let [sym (symbol class-name)
	keys (map (fn [[key type]] (symbol (attribute-key key))) attributes)]
    (eval `(do (defrecord ~sym ~keys) ~sym))))

(defn ^Creator record-creator [^Class class]
  "make a Creator for the given Class"
  (reify Creator (create [_ args] (apply make-instance class args))))

(defn ^Getter record-getter [^Class input-type ^Class output-type name]
  "make a Getter for the named attribute of the given Class"
  (let [key (keyword (attribute-key name))
	arg-symbol (with-meta 's {:tag (.getCanonicalName input-type)})]
    (eval `(reify Getter (get [_ ~arg-symbol] (~key ~arg-symbol))))))


(defn ^Metadata record-metadata2 [primary-keys version-keys version-comparator attributes]
  "make a record-based Metadata instance"
  (let [class (record-class attributes)]
    (new MetadataImpl
	 (record-creator class)
	 primary-keys
         nil
	 version-keys
	 version-comparator
	 (map (fn [[key type mutable]] (Attribute. key type mutable (record-getter class type key))) attributes))))

(defn ^Metadata named-record-metadata2 [class-name primary-keys version-keys version-comparator attributes]
  "make a record-based Metadata instance"
  (let [class (named-record-class class-name attributes)]
    (new MetadataImpl
	 (record-creator class)
	 primary-keys
         nil
	 version-keys
	 version-comparator
	 (map (fn [[key type mutable]] (Attribute. key type mutable (record-getter class type key))) attributes))))

(let [record-metadata-cache (atom {})]

  (defn ^Metadata record-metadata
    "return memoized record-metadata"
    [primary-keys version-keys version-comparator attributes]
    (let [cache-key [primary-keys version-keys version-comparator attributes]]
      ((swap!
	record-metadata-cache 
	(fn [cache key]
	    (if (contains? cache key) cache (assoc cache key (record-metadata2 primary-keys version-keys version-comparator attributes))))
	cache-key)
       cache-key)))

  (defn ^Metadata named-record-metadata
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

;; TODO - creator should take a collection OR an array ?
(defmacro make-record-creator [class-name field-names]
  `(reify Creator
     (create [_ args#]
      (let [~(apply vector field-names) args#] (~(symbol (str (name class-name) "." )) ~@field-names)))
     ))

(defmacro make-record-getter [field-name input-type output-type]
  `(reify Getter (get [_ datum#] (~(symbol (str "." field-name)) datum#))))

;; this can't be right but I cannot figure out a better way to get the same result...
(defmacro make-key-fn [input-type keys]
  (list 'fn [(with-meta 'v {:tag input-type})]
	(apply (if (= (count keys) 1) identity vector) (map (fn [key] (list (symbol (str "." key)) 'v)) keys))))
	
(defmacro make-version-comparator [input-type version-keys version-comparator]
  `(let [version-fn# (make-key-fn ~input-type ~version-keys)]
     (reify Metadata$VersionComparator
 	    (compareTo [_ lhs# rhs#] (~version-comparator (version-fn# lhs#) (version-fn# rhs#))))))

(def prim->ref
     {'int 'Integer
     'long 'Long
     'float 'Float
     'double 'Double
     'void 'Void
     'short 'Short
     'boolean 'Boolean
     'byte 'Byte
     'char 'Character})

(defmacro defrecord-metadata [var-name class-name fields & [version-comparator]]
  (let [primary-keys (filter (fn [field] (:primary-key (meta field))) fields)
	version-keys (filter (fn [field] (:version-key (meta field))) fields)]
    
    `(do
       (defrecord ~class-name ~fields)
       (def ^Metadata ~var-name
	    (MetadataImpl.
	     (make-record-creator ~class-name ~fields)
	     (list ~@(map keyword primary-keys))
             (make-key-getter ~class-name ~primary-keys)
	     (list ~@(map keyword version-keys))
	     (make-version-comparator ~class-name ~version-keys ~version-comparator)
	     (list
	      ~@(map 
		 (fn [field]
		     (let [m (meta field)
			   tag (:tag m)
			   tag (get prim->ref tag tag)]
		       `(Attribute.
			 ~(keyword field)
			 ~tag
			 ~(not (or (:primary-key m) (:immutable m)))
			 (make-record-getter ~field ~class-name ~tag))))
		 fields))
	      ))
	    )))

(defn fix-field [s] (with-meta (symbol (.replace (name s) "-" "_")) (meta s)))

(defmacro definterface-metadata [var-name class-name fields & [version-comparator]]
  (let [primary-keys (filter (fn [field] (:primary-key (meta field))) fields)
	version-keys (filter (fn [field] (:version-key (meta field))) fields)]
    
    `(do
       (definterface
           ~class-name
         ~@(map (fn [field] `(~(fix-field field) [])) fields))
       (def ^Metadata ~var-name
	    (MetadataImpl.
	     nil ;; (make-record-creator ~class-name ~fields)
	     (list ~@(map keyword primary-keys))
             (make-key-getter ~class-name ~primary-keys)
	     (list ~@(map keyword version-keys))
	     (make-version-comparator ~class-name ~version-keys ~version-comparator)
	     (list
	      ~@(map 
		 (fn [field]
		     (let [m (meta field)
			   tag (:tag m)
			   tag (get prim->ref tag tag)]
		       `(Attribute.
			 ~(keyword field)
			 ~tag
			 ~(not (or (:primary-key m) (:immutable m)))
			 (make-record-getter ~field ~class-name ~tag))))
		 fields))
	      ))
	    )))
;;--------------------------------------------------------------------------------

(defn sql-attributes 
  "Derive a sequence of MetaData attributes from an SQL ResultSet"
  [^ResultSetMetaData sql-metadata type-translations]
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
  [^Metadata metadata ^ResultSet result-set type-translations]
  (let [creator (.getCreator metadata)
	readers (map
		 (fn [^Attribute attribute]
		     (let [[_ translator] (type-translations (.getKey attribute))]
		       (or translator
			   (fn [^ResultSet result-set ^Integer column-index] (.getObject result-set column-index)))))
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
	     (fn [reader ^Integer column-index] (reader result-set column-index))
	     readers
	     (iterate inc 1))))
	  output))
	output))))

;;--------------------------------------------------------------------------------

(defn model [^String model-name ^Metadata metadata]
  (SimpleModelView. model-name metadata))

;; this should really be collapsed into (model) above - but arity overloading is not sufficient...
(defn clone-model [^Model model ^String name]
  (SimpleModelView. name (.getMetadata model)))

(defn sql-model
  "make a SQL query and return the ResultSet as a Model"
  [model-name ^Connection connection ^String sql primary-keys version-keys version-comparator & [type-translations]]
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
                         nil            ;primary-getter
			 []		;version-keys
			 (reify Metadata$VersionComparator (compareTo [_ old new] -1)) ;version-comparator
			 [(Attribute. :name String false (reify Getter (get [_ model] (.getName ^Model model))))])) ;attributes

;; TODO: I like the earmuffs on this var, but have no intention of
;; rebinding its root at runtime - hence I need the :dynamic hint to
;; avoid compiler warnings
(def ^:dynamic ^Model *metamodel* (SimpleModelView. "MetaModel" metamodel-metadata))
(insert *metamodel* *metamodel*)

;; TODO - this could be ASYNC - should be loaded from SPRING CONFIG
(def ^ServiceFactory internal-view-service-factory (SynchronousServiceFactory.))
