(require :reload '[cljs.compiler :as comp] '[clojure.java.io :as io] '[cljs.core])
(import '(com.google.javascript.jscomp JSSourceFile CompilerOptions CompilationLevel 
                                       ClosureCodingConvention))

(let [in "src/main/script/org/healthsciencessc/rpms2/core.cljs"
    	out "src/main/script/org/healthsciencessc/rpms2/core.js"
      target "src/main/resources/public/app.js"
      options []]
  (comp/compile-file in out)
  (let [source (slurp out)
        options (CompilerOptions.)
        level CompilationLevel/WHITESPACE_ONLY
        extern (JSSourceFile/fromCode "externs.js" "function alert(x) {}")
        input (JSSourceFile/fromCode "input.js" source)
        compiler (com.google.javascript.jscomp.Compiler.)]
    (.setCodingConvention options (ClosureCodingConvention.))
    (.setPrettyPrint options true)
    (.compile compiler extern input options)
    (spit target (.toSource compiler))))