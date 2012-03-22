(ns org.healthsciencessc.rpms2.consent-domain.codec
  (:import [java.security NoSuchAlgorithmException MessageDigest]
           [java.math BigInteger]
           [javax.crypto Cipher]
           [javax.crypto.spec SecretKeySpec]
           [org.apache.commons.codec.binary Hex Base64]))


(defn byte-array?
  "Checks if the provided value is a byte array"
  [value]
  (= (type value) (type (byte-array 0))))

(defn bytify
  "Coerces a value into a byte arry.  Specirically for getting bytes from a string using the appropriate character set."
  [value]
  (if (byte-array? value) value (.getBytes value Hex/DEFAULT_CHARSET_NAME)))

(defn stringify
  "Coerces a value into a string.  Specifically for use with byte arrays using the approriate character set."
  [value]
  (if (byte-array? value) (new String value Hex/DEFAULT_CHARSET_NAME)(.toString value)))

(defn byteme
  "Takes a string and an int and returns an array of bytes the size of the int filed with bytes 
   from the string.  If the string byte length is shorter than the required length it's bytes 
   are reused over and over until the array is filled."
  [filler length]
  (if (> length (.length filler))
    (recur (str filler filler) length)
    (byte-array (take length (seq (bytify filler))))))


(defn spec-key
  "Generates a javax.crypto.spec.SecretKeySpec using provided properties from a map.  The process 
   expects the map to contain the following:
   
     passcode    : The passcode used to generate the key spec, hence the actual bits/salt that is 
                   is used in the encryption.  REQUIRED.
     algorithm   : The algorithm to use, such as AES, DAS, etc.  Default is AES.
     bits        : The size of the key to generate.  128, 196, 256.  Default is 128
"
  ([passcode] (spec-key passcode {}))
  ([passcode options]
  (let [algorithm (:algorithm options "AES")
        bits (:bits options 128)]
      (if (or (= nil passcode) (empty passcode)) (throw (new IllegalArgumentException "A passcode is required to generate a secret key specification")))
      (let [bites (/ bits 8)
            pass (byteme passcode bites)
            spec (SecretKeySpec. pass algorithm)]
        spec))))


(defn encrypt
  "Encrypts a provided byte array using a given SecretKeySpec"
  [value keyspec]
  (let [bites (bytify value)
        cipher (Cipher/getInstance (.getAlgorithm keyspec))]
    (.init cipher Cipher/ENCRYPT_MODE keyspec)
    (.doFinal cipher bites))) ; Always return bytes


(defn decrypt
  "Encrypts a byte array using a given SecretKeySpec"
  [value keyspec]
  (let [bites (bytify value)
        cipher (Cipher/getInstance (.getAlgorithm keyspec))]
    (.init cipher Cipher/DECRYPT_MODE keyspec)
    (.doFinal cipher bites))) ; Always return bytes


(defn decrypter
  "Creates a function that wraps the keyspec and can be reused over and over."
  ([passkey] (decrypter passkey {}))
  ([passkey options]
    (let [keyspec (if (isa? (type passkey) SecretKeySpec) passkey (spec-key passkey options))]
      (fn [value] (decrypt value keyspec)))))


(defn encoded-base64?
  "Tests if a value is encoded as plain encrypted text, which is indicated by
   surrounding it with 'ENC(' and ')' tokens."
  [value]
  (and (.startsWith value "ENC(") (.endsWith value ")")))


(defn encoded-hex?
  "Tests if a value is encoded as plain encrypted text, which is indicated by
   surrounding it with 'ENC(' and ')!' tokens."
  [value]
  (and (.startsWith value "ENC(") (.endsWith value ")!")))


(defn encoded?
  "Tests is a value is encoded as encoded-plain? or encoded-hex?"
  [value]
  (or (encoded-base64? value) (encoded-hex? value)))


(defn encode-base64
  "Encodes a string with the basic 'ENC(%)' wrapper."
  [value]
  (let [bites (bytify value)]
    (str "ENC(" (Base64/encodeBase64String bites) ")")))


(defn encode-hex
  "Encodes a string with the basic 'ENC(%)!' wrapper."
  [value]
  (let [bites (bytify value)]
    (str "ENC(" (Hex/encodeHexString bites) ")!")))

(defn decode
  "Removed any encoded tags from the value and returns the content as a decoded but 
   still encrypted byte array"
  ([value] (decode value stringify))
  ([value fx]
  (cond 
    (encoded-hex? value) (let [hex (apply str (butlast (butlast (drop 4 value))))
                               letters (.toCharArray hex)]
                           (stringify (fx (Hex/decodeHex letters))))
    (encoded-base64? value) (let [base (apply str (butlast (drop 4 value)))]
                           (stringify (fx (Base64/decodeBase64 base))))
    :else  value)))


