(ns
 org.dada.core
 (:use org.dada.core.UnionModel)
 (:import (clojure.lang
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
	  (org.slf4j Logger LoggerFactory)
	  (org.dada.asm ClassFactory)
	  (org.dada.core
	   AbstractModel
	   Attribute
	   Creator
	   Getter
	   MetaModel
	   MetaModelImpl
	   Metadata
	   MetadataImpl
	   Model
	   ServiceFactory
	   StringMetadata
	   UnionModel
	   Update
	   View
	   )
	  (org.dada.demo Client)
	  ))

;;--------------------------------------------------------------------------------

(defn trace [& args]
  ;;(apply println args)
  )

(defn debug [& foo]
  (println "DEBUG: " foo)
  foo)

(defn warn [& args]
  (println "WARN: " args)
  args)

;;--------------------------------------------------------------------------------

(set! *warn-on-reflection* true)

(def #^Logger *logger* (LoggerFactory/getLogger "org.dada.core"))

;; TODO: Spring should look after this - see application-context.xml...
(if (not (System/getProperty "dada.broker.name")) (System/setProperty "dada.broker.name" "DADA"))
(if (not (System/getProperty "dada.broker.uri")) (System/setProperty "dada.broker.uri" "tcp://localhost:61616"))

(do
  (def #^ServiceFactory *external-metamodel-service-factory* nil)
  (def #^ServiceFactory *external-model-service-factory* nil)
  (def #^ServiceFactory *internal-view-service-factory* nil)
  (def #^Lock *exclusive-lock* nil)
  (def #^MetaModel *metamodel* nil))


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
  (.registerView model view)
  (let [batch (map #(Update. nil %) (.getData #^AbstractModel model))] ;; TODO: getData should be on Model ?
    (if (not (empty? batch)) (.update view batch '() '())))
  view)

(defn disconnect [#^Model model #^View view]
  (.update
   view
   '()
   '()
   (map #(Update. % nil) (.deregisterView model view)))
  view)

(defmulti attribute-key (fn [arg] (class arg)))
(defmethod attribute-key Date [#^Date date] (attribute-key (str "attribute" date)))
(defmethod attribute-key clojure.lang.Keyword [#^Keyword keyword] (attribute-key (name keyword)))

(def string-replacements {
     "_" "_underscore_"
     " " "_space_"
     ":" "_colon_"
     "*" "_asterisk_"
     "+" "_plus_"
     "-" "_minus_"
     "/" "_divide_"
     "(" "_openroundbracket_"
     ")" "_closeroundbracket_"
     })

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

(def *exported-classes* (atom {}))

(defn custom-class [#^String class-name #^Class superclass #^ISeq & attribute-key-types]
  (let [bytes (.create
	       *custom-class-factory*
	       class-name
	       superclass
	       (apply custom-attribute-array attribute-key-types))]
    (swap! *exported-classes*  (fn [classes] (assoc classes class-name bytes)))
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

(defn #^Metadata custom-metadata3 [#^Class class keys attribute-specs]
  "make Metadata for a given class"
  (new MetadataImpl
       (custom-creator class)
       keys
       (map
	(fn [[key type mutable]] (Attribute. key type mutable (custom-getter class type key)))
	attribute-specs)))

(defn #^Metadata custom-metadata2
  "create metadata for a Model containing instances of a Class"
  [#^String class-name #^Class superclass #^Collection keys #^Collection attributes]
  (let [class-attributes (mapcat (fn [[key type _]] [key type]) attributes)]
    (custom-metadata3 (apply custom-class class-name superclass class-attributes) keys attributes)))

(let [custom-metadata-cache (atom {})]

  (defn #^Metadata custom-metadata
    "create metadata for a Model containing instances of a Class"
    [#^String class-name #^Class superclass #^Collection keys #^Collection attributes]
    (let [cache-key [superclass keys attributes]]
      ((swap!
	custom-metadata-cache 
	(fn [cache key]
	    (if (contains? cache key) cache (assoc cache key (custom-metadata2 class-name superclass keys attributes))))
	cache-key)
       cache-key)))

  )

;;--------------------------------------------------------------------------------
;; handle sequences

(defn #^Metadata seq-metadata [length]
  (new MetadataImpl
       (proxy [Creator] [] (create [args] args))
       [0]
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


(defn #^Metadata record-metadata2 [keys attributes]
  "make a record-based Metadata instance"
  (let [class (record-class attributes)]
    (new MetadataImpl
	 (record-creator class)
	 keys
	 (map (fn [[key type mutable]] (Attribute. key type mutable (record-getter class type key))) attributes))))


(let [record-metadata-cache (atom {})]

  (defn #^Metadata record-metadata
    "return memoized record-metadata"
    [keys attributes]
    (let [cache-key [keys attributes]]
      ((swap!
	record-metadata-cache 
	(fn [cache key]
	    (if (contains? cache key) cache (assoc cache key (record-metadata2 keys attributes))))
	cache-key)
       cache-key)))

  )

;;--------------------------------------------------------------------------------

(defn sql-attributes 
  "Derive a sequence of MetaData attributes from a SQL ResultSet"
  [#^ResultSetMetaData sql-metadata]
  (map
   (fn [column]
       [(keyword (.getColumnName sql-metadata column))
	(Class/forName (.getColumnClassName sql-metadata column))
	true])			 ;we don't know - so assume mutability
   (range 1 (+ 1 (.getColumnCount sql-metadata)))))

(defn sql-data
  "Create a sequence from some MetaData and a SQL ResultSet"
  [#^Metadata metadata #^ResultSet result-set]
  (let [creator (.getCreator metadata)]
    (loop [output nil]
      (if (.next result-set)
	(recur
	 (cons
	  (.create 
	   creator 
	   (into-array
	    Object
	    (map 
	     (fn [#^Integer column] (.getObject result-set column))
	     (range 1 (+ 1 (count (.getAttributes metadata)))))))
	  output))
	output))))

;;--------------------------------------------------------------------------------


(defn model [#^String model-name version-key #^Metadata metadata]
  (let [version-fn (if version-key
		     (let [version-getter (.getGetter (.getAttribute metadata version-key))]
		       (fn [old new] (> (.get version-getter new) (.get version-getter old))))
		     (fn [_ new] new))]
    (UnionModel. model-name metadata version-fn))) ;; version-fn should be retrieved from metadata

;; this should really be collapsed into (model) above - but arity overloading is not sufficient...
(defn clone-model [#^Model model #^String name]
  (let [metadata (.getMetadata model)
	keys (.getKeyAttributeKeys metadata)
	key-getter (.getGetter (.getAttribute metadata (first keys)))
	version-getter (.getGetter (.getAttribute metadata (second keys)))]
    (UnionModel.
     name
     metadata
     (fn [old new] (> (.get version-getter new) (.get version-getter old))))))

(defn sql-model
  "make a SQL query and return the ResultSet as a Model"
  [model-name #^Connection connection #^String sql]
  (let [result-set (.executeQuery (.prepareStatement connection sql))
	metadata (record-metadata [:id] (sql-attributes (.getMetaData result-set))) ;lets use records...
	data (sql-data metadata result-set)
	sql-model (model model-name nil metadata)]
    (.close result-set)
    (insert-n sql-model data)
    sql-model))

;;--------------------------------------------------------------------------------

(defn start-server []

  (do
    (def #^ClassPathXmlApplicationContext *spring-context* (ClassPathXmlApplicationContext. "application-context.xml"))
    (def #^ServiceFactory *external-metamodel-service-factory* (.getBean #^BeanFactory *spring-context* "externalMetaModelServiceFactory"))
    (def #^ServiceFactory *external-model-service-factory* (.getBean #^BeanFactory *spring-context* "externalModelServiceFactory"))
    (def #^ServiceFactory *internal-view-service-factory* (.getBean #^BeanFactory *spring-context* "internalViewServiceFactory"))
    (def #^Lock *exclusive-lock* (.getBean #^BeanFactory *spring-context* "writeLock"))

    (def #^MetaModel *metamodel*
	 (MetaModelImpl.
	  (str (System/getProperty "dada.broker.name") ".MetaModel") 
	  (StringMetadata. "Name")
	  ;;(custom-metadata3 String ["Name"] [["Name" String false]])
	  *external-metamodel-service-factory*))
    (insert *metamodel* *metamodel*)
    )

  (let [metamodel-name (.getName *metamodel*)]
    (.start *metamodel*)
    (println "Server - modelling:" metamodel-name)
    (.server *external-metamodel-service-factory* *metamodel* metamodel-name)))

(defn start-client []
  (Client/main (into-array String (list (System/getProperty "dada.broker.name")))))

;;--------------------------------------------------------------------------------
