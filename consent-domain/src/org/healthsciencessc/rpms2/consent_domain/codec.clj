(ns org.healthsciencessc.rpms2.consent-domain.codec
  (:use 'clojure.core )
  (:require 'clojure.contrib.str-utils2 :as utils)
  (:import [java.security NoSuchAlgorithmException MessageDigest]
           [java.math BigInteger]
           [javax.crypto Cipher]
           [javax.crypto.spec SecretKeySpec]
           [org.apache.commons.codec.binary Hex]))


(defn byte-array?
  "Checks if the provided value is a byte array"
  [value]
  (= (type value) (type (byte-array 0))))

(defn bytify
  "Gets the bytes from a string using the appropriate character set."
  [string]
  (.getBytes string Hex/DEFAULT_CHARSET_NAME))

(defn stringify
  "Converts byte array into string using the approriate character set."
  [bites]
  (new String bites Hex/DEFAULT_CHARSET_NAME))

(defn byteme
  "Takes a string and an int and returns an array of bytes the size of the int filed with bytes 
   from the string.  If the string byte length is shorter than the required length it's bytes 
   are reused over and over until the array is filled."
  [filler length]
  (if (> length (.length filler))
    (recur (str filler filler) length)
    (byte-array (take length (seq (.getBytes filler))))))


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
  (let [bites (if (byte-array? value) value (.getBytes value))
        cipher (Cipher/getInstance (.getAlgorithm keyspec))]
    (.init cipher Cipher/ENCRYPT_MODE keyspec)
    (let [enc (.doFinal cipher bites)]
      (if (byte-array? value) ; Return type that was provided
        enc                   ; Provided was byte array
        (new String enc)))))  ; Provided was string



(defn decrypt
  "Encrypts a byte array using a given SecretKeySpec"
  [value keyspec]
  (let [bites (if (byte-array? value) value (.getBytes value))
        cipher (Cipher/getInstance (.getAlgorithm keyspec))]
    (.init cipher Cipher/DECRYPT_MODE keyspec)
    (let [enc (.doFinal cipher bites)]
      (if (byte-array? value) ; Return type that was provided
        enc                   ; Provided was byte array
        (new String enc)))))  ; Provided was string


(defn decrypter
  "Creates a function that wraps the keyspec and can be reused over and over."
  ([passkey] (decrypter passkey {}))
  ([passkey options]
    (let [keyspec (spec-key passkey options)]
      (fn [value] (decrypt value keyspec)))))


(defn encoded-plain?
  "Tests if a value is encoded as plain encrypted text, which is indicated by
   surrounding it with 'ENC(' and ')' tokens."
  [value]
  (and (.startsWith value "ENC(") (.endsWith value ")")))


(defn encoded-hex?
  "Tests if a value is encoded as plain encrypted text, which is indicated by
   surrounding it with 'ENC(' and ')' tokens."
  [value]
  (and (.startsWith value "ENC(") (.endsWith value ")!")))


(defn encoded?
  "Tests is a value is encoded as encoded-plain? or encoded-hex?"
  [value]
  (or (encoded-plain? value) (encoded-hex? value)))


(defn encode
  "Encodes a string with the basic 'ENC(%)' wrapper."
  [value]
  (let [string (if (byte-array? value) (stringify value) (.toString value))]
    (str "ENC(" string ")")))


(defn encode-hex
  "Encodes a string with the basic 'ENC-HEX(%)' wrapper."
  [value]
  (let [bites (if (byte-array? value) value (bytify value))]
    (str "ENC(" (Hex/encodeHexString bites) ")!")))

(defn decode
  "Removed any encoded tags from the value and returns the content as a decoded but 
   still encrypted byte array"
  ([value] (decode value str))
  ([value fx]
  (cond 
    (encoded-hex? value) (let [hex (utils/drop (utils/chop (utils/chop value)) 4)
                               letters (.toCharArray hex)]
                           (fx (stringify (Hex/decodeHex letters))))
    (encoded-plain? value) (str (utils/drop (utils/chop value) 4))
    :else  value)))


