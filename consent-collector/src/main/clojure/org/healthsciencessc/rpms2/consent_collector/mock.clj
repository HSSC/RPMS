(ns org.healthsciencessc.rpms2.consent-collector.mock)

(def lewis-blackman-form-orig
{:form
 {:collect-start "page1",
  :summary-start "spage1",
  :header {:title "Lewis Blackman Hospital Patient Safety Act Acknowledgement"},
  :contains
  [
   {:name "page1",
    :title "Acknowledgement",
    :type "page",
    :contains
    [{:name "section1",
      :type "section",
      :contains
      [{:text
        ["Verify the information below and use corresponding change button to correct as needed."],
        :name "info",
        :title "Verify Information",
        :type "text"}
       {:meta-items ["MI001" "MI002"], :name "", :type "data-change"}]}
     {:name "section2",
      :type "section",
      :contains
      [{:render-title false,
        :name "Acknowledge",
        :policy "P00001",
        :type "policy-text",
        :render-text true}
       {:endorsement "E00001",
        :name "Sign Here",
        :type "signature",
        :clear-label "Clear"}]}]}

   {:name "spage1",
    :title "REIVEW Acknowledgement",
    :type "page",
    :contains
    [{:name "section1",
      :type "section",
      :contains
      [{:text
        ["THIS IS IN REVIEW Verify the information below and use corresponding change button to correct as needed."],
        :name "info",
        :title "IN REVIEW Verify Information",
        :type "text"}
       {:meta-items ["MI001" "MI002"], :name "", :type "data-change"}]}
     {:name "section2",
      :type "section",
      :contains
      [{:render-title false,
        :name "Acknowledge",
        :policy "P00001",
        :type "review-policy",
        :returnpage "page1"
        :label "Edit"
        :endorsement-label "In REVIEW: Endorsement Label for Acknowledge"
        :render-text true}
       {:endorsement "E00001",
        :name "Review Sign Here",
        :title "Title of Review Sign Here",
        :label "Edit",
        :type "review-endorsement",
        :clear-label "Clear"}]}]}
   ],
  :footer {:title ["Patient Name: Bob R. Smith"]}},
 :meta-items
 {:MI002 {:label "Gaurantor", :value "Medical Services Foundation"},
  :MI001 {:label "Admission Date", :value "05-24-2012"}
  },
 :policies
 {:P00001
  {:text
   [
"I understand that MUSC is an academic medical center providing healthcare, teaching, and research. I also understand that I may be cared for by physicians and other healthcare providers in training. My care will be supervised by an attending physician and staff. The information related to the health care I receive may be used for training or for scientific study purposes. I understand that if information contained in my health care record is used for such purposes, precautions will be taken to carefully preserve my anonymity. I also understand that pictures or other recordings may be made for purposes of my treatment or for educational purposes."
"In addition, I understand that if I have questions about my medical care I can speak to my attending physician, nurse, resident, or medical student. In addition, I can contact my attending physician or the House Services Coordinator through the operator at 792-8080 (2-8080 from the bedside phone)."
"I acknowledge that I have received a copy of the brochure entitled \"Medical University of South Carolina, An Academic Medical Center of Excellence\" explaining the role of residents and students in my care."
],
   :title "Your Acknowledgement Is Needed" }
  },
 :endorsements
 {:E00001
  {:endorsement "the base64/png encoded signature value.",
   :label "Signature of Consenter"}}}
)



(def sample-form-orig

{:form
 {:collect-start "page1",
  :summary-start "summary1",
  :header {:title "Consent For Medical Treatment"},
  :contains
  [{:name "page1",
    :title "Consent To Treat Me",
    :type "page",
    :contains
    [{:name "section1",
      :type "section",
      :contains
      [{:render-title true,
        :name "MedicalTreatmentText",
        :policy "P00001",
        :render-media true,
        :type "policy-text",
        :render-text true}
       {:name "MedicalTreatmentChoiceButtons",
        :policy "P00001",
        :type "policy-choice-buttons",
        :false-label "I do not agree",
        :true-label "I Agree"}]}
     {:name "section2",
      :type "section",
      :contains
      [{:text
        ["If either of these are not correct, please press the corresponding change button."],
        :name "ReferringDoctorText",
        :title "Release of Information to My Referring Doctor",
        :type "text"}]}
     {:name "section3",
      :type "section",
      :contains
      [{:meta-items ["MI002" "MI001"],
        :name "flagThemDoctors",
        :type "data-change"}]}],
    :next "page2"}
   {:name "page2",
    :title "Consent To Pay",
    :type "page",
    :contains
    [{:name "section1",
      :type "section",
      :contains
      [{:name "PaymentText", :policy "P00003", :type "policy-text"}
       {:name "PaymentChoiceButtons",
        :policy "P00003",
        :type "policy-choice-buttons",
        :false-label "I do not agree",
        :true-label "I Agree"}]}],
    :previous "page1",
    :next "page3"}
   {:name "page3",
    :title "Tissue Retention Disposal",
    :type "page",
    :contains
    [{:name "section1",
      :type "section",
      :contains
      [{:name "TissueText", :policy "P00004", :type "policy-text"}
       {:name "TissueCheckbox",
        :policy "P00004",
        :type "policy-checkbox",
        :unchecked-value true,
        :checked-value false,
        :label
        "I DO NOT agree to have my tissue or blood used for future research studies."}]}],
    :previous "page2",
    :next "page4"}
   {:name "page4",
    :title "Permission to Contact",
    :type "page",
    :contains
    [{:name "section1",
      :type "section",
      :contains
      [{:name "PermissionText", :policy "P00005", :type "policy-text"}
       {:name "PermissionCheckbox",
        :policy "P00005",
        :type "policy-checkbox",
        :unchecked-value true,
        :checked-value false,
        :label
        "I DO NOT agree to be contacted for future research studies."}]}],
    :previous "page3",
    :next "page5"}
   {:name "page5",
    :title "Photographs",
    :type "page",
    :contains
    [{:name "section1",
      :type "section",
      :contains
      [{:name "PhotographText", :policy "P00006", :type "policy-text"}
       {:name "PhotographChoiceButtons",
        :policy "P00006",
        :type "policy-choice-buttons",
        :false-label "I do not agree",
        :true-label "I Agree"}]}],
    :previous "page4",
    :next "page6"}
   {:name "page6",
    :title "PersonalItems",
    :type "page",
    :contains
    [{:name "section1",
      :type "section",
      :contains
      [{:name "PersonalItemsText",
        :policy "P00007",
        :type "policy-text"}
       {:name "PersonalItemsButton",
        :policy "P00007",
        :type "policy-button",
        :label "I Understand",
        :action-value true}]}],
    :previous "page5",
    :next "page7"}
   {:name "page7",
    :title "Copy of Privacy Practices",
    :type "page",
    :contains
    [{:name "section1",
      :type "section",
      :contains
      [{:name "PrivacyPracticesText",
        :policy "P00008",
        :type "policy-text"}
       {:name "PrivacyPracticesChoiceButtons",
        :policy "P00008",
        :type "policy-choice-buttons",
        :false-label "I do not agree",
        :true-label "I Agree"}]}],
    :previous "page6",
    :next "page8"}
   {:name "page8",
    :title "Consent Certifications",
    :type "page",
    :contains
    [{:name "section1",
      :type "section",
      :contains
      [{:render-title false,
        :name "ConsentText",
        :policy "P00009",
        :type "policy-text"}
       {:name "ConsentChoiceButtons",
        :policy "P00009",
        :type "policy-choice-buttons",
        :false-label "I do not agree",
        :true-label "I Agree"}]}
     {:name "section2",
      :type "section",
      :contains
      [{:endorsement "E00001",
        :name "consenter",
        :type "signature",
        :clear-label "Clear"}]}],
    :previous "page7",
    :next "page9"}

   {:name "summary1",
    :title "REVIEW Consent To Treat Me",
    :type "page",
    :contains
    [{:name "section1",
      :type "section",
      :contains
      [{:render-title true,
        :name "MedicalTreatmentText",
        :policy "P00001",
        :render-media true,
        :type "policy-text",
        :render-text true}

       {:name "MedicalTreatmentChoiceButtons",
        :policy "P00001",
        :type "review-policy"
        :returnpage "page1"
        :label "Edit"
       }
       ]}
     ],
    :next "spage6"}




   {:name "spage2",
    :title "REVIEW Consent To Pay",
    :type "page",
    :contains
    [{:name "section1",
      :type "section",
      :contains
      [{:name "PaymentText", :policy "P00003", :type "policy-text"}
       {:name "PaymentChoiceButtons",
        :policy "P00003",
        :type "policy-choice-buttons",
        :false-label "I do not agree",
        :true-label "I Agree"}]}],
    :previous "summary1",
    :next "spage3"}
   {:name "spage3",
    :title "Tissue Retention Disposal",
    :type "page",
    :contains
    [{:name "section1",
      :type "section",
      :contains
      [{:name "TissueText", :policy "P00004", :type "policy-text"}
       {:name "TissueCheckbox",
        :policy "P00004",
        :type "policy-checkbox",
        :unchecked-value true,
        :checked-value false,
        :label
        "I DO NOT agree to have my tissue or blood used for future research studies."}]}],
    :previous "spage2",
    :next "spage4"}
   {:name "spage4",
    :title "Permission to Contact",
    :type "page",
    :contains
    [{:name "section1",
      :type "section",
      :contains
      [{:name "PermissionText", :policy "P00005", :type "policy-text"}
       {:name "PermissionCheckbox",
        :policy "P00005",
        :type "policy-checkbox",
        :unchecked-value true,
        :checked-value false,
        :label
        "I DO NOT agree to be contacted for future research studies."}]}],
    :previous "spage3",
    :next "spage5"}
   {:name "spage5",
    :title "Photographs",
    :type "page",
    :contains
    [{:name "section1",
      :type "section",
      :contains
      [{:name "PhotographText", :policy "P00006", :type "policy-text"}
       {:name "PhotographChoiceButtons",
        :policy "P00006",
        :type "policy-choice-buttons",
        :false-label "I do not agree",
        :true-label "I Agree"}]}],
    :previous "spage4",
    :next "spage6"}
   {:name "spage6",
    :title "PersonalItems",
    :type "page",
    :contains
    [{:name "section1",
      :type "section",
      :contains
      [{:name "PersonalItemsText",
        :policy "P00007",
        :type "policy-text"}

       {:name "PersonalItemsButton",
        :policy "P00007",
        :type "review-policy",
        :returnpage "page6" 
        :title "This is an optional title"
        :label "Edit",
        }]}],
    :previous "spage5",
    :next "spage7"}
   {:name "spage7",
    :title "Copy of Privacy Practices",
    :type "page",
    :contains
    [{:name "section1",
      :type "section",
      :contains
      [{:name "PrivacyPracticesText",
        :policy "P00008",
        :type "policy-text"}
       {:name "PrivacyPracticesChoiceButtons",
        :policy "P00008",
        :type "policy-choice-buttons",
        :false-label "I do not agree",
        :true-label "I Agree"}]}],
    :previous "spage6",
    :next "spage8"}
   {:name "spage8",
    :title "Consent Certifications",
    :type "page",
    :contains
    [{:name "section1",
      :type "section",
      :contains
      [{:render-title false,
        :name "ConsentText",
        :policy "P00009",
        :type "policy-text"}
       {:name "ConsentChoiceButtons",
        :policy "P00009",
        :type "policy-choice-buttons",
        :false-label "I do not agree",
        :true-label "I Agree"}]}
     {:name "section2",
      :type "section",
      :contains
      [{:endorsement "E00001",
        :name "consenter",
        :type "signature",
        :clear-label "Clear"}]}],
    :previous "spage7",
    :next "spage9"}],
  :footer {:title ["Patient Name: Bob R. Smith"]}}} )


(def lewis-blackman-form
{:form
 {:collect-start "page1",
  :summary-start "summary1",
  :header {:title "Lewis Blackman Hospital Patient Safety Act Acknowledgement"},
  :contains
  [{:name "page1",
    :title "Acknowledgement",
    :type "page",
    :contains
    [{:name "section1",
      :type "section",
      :contains
      [{:text
        ["Verify the information below and use corresponding change button to correct as needed."],
        :name "info",
        :title "Verify Information",
        :type "text"}
       {:meta-items ["MI001" "MI002"], :name "", :type "data-change"}]}
     {:name "section2",
      :type "section",
      :contains
      [{:render-title false,
        :name "Acknowledge",
        :policy "P00001",
        :type "policy-text",
        :render-text true}
       {:endorsement "E00001",
        :name "Sign Here",
        :type "signature",
        :clear-label "Clear"}]}]}

    {:name "summary1",
    :title "Review Permissions",
    :type "page",
    :contains
    [{:name "section1",
      :type "section",
      :contains
      [{:type "review-metaitem",
   	    :name "ReviewMetaItem1",
    	:title "Review Information",         
        :meta-item "MI001",           
    	:label "Edit"}
	   {:type "review-metaitem",
   	    :name "ReviewMetaItem2",
    	:title "Review Information",         
        :meta-item "MI002",           
    	:label "Edit"}]}
	 {:name "section2",
      :type "section",
      :contains
      [{:type "review-policy",
       :name "ReviewPolicy",
    	:title "Review Acknowledgement",         
        :policy "P00001",           
    	:label "Edit",             
    	:returnpage "page1"}]}

     {:name "section3",
      :type "section",
      :contains
      [{:type "review-endorsement", 
        :name "ReviewEndorsement", 
    	:title "Review Signature",
    	:endorsement "E00001",
    	:label "Edit",
    	:returnpage "page1"}
       ]}]}
   ],
  :footer {:title ["Patient Name: Bob R. Smith"]},
 :meta-items
 {:MI002 {:label "Gaurantor", :value "Medical Services Foundation"},
  :MI001 {:label "Admission Date", :value "05-24-2012"}
  },
 :policies
 {:P00001
  {:text
   [
"I understand that MUSC is an academic medical center providing healthcare, teaching, and research. I also understand that I may be cared for by physicians and other healthcare providers in training. My care will be supervised by an attending physician and staff. The information related to the health care I receive may be used for training or for scientific study purposes. I understand that if information contained in my health care record is used for such purposes, precautions will be taken to carefully preserve my anonymity. I also understand that pictures or other recordings may be made for purposes of my treatment or for educational purposes."
"In addition, I understand that if I have questions about my medical care I can speak to my attending physician, nurse, resident, or medical student. In addition, I can contact my attending physician or the House Services Coordinator through the operator at 792-8080 (2-8080 from the bedside phone)."
"I acknowledge that I have received a copy of the brochure entitled \"Medical University of South Carolina, An Academic Medical Center of Excellence\" explaining the role of residents and students in my care."
],
   :title "Your Acknowledgement Is Needed" }
  },
 :endorsements
 {:E00001
  {:endorsement "the base64/png encoded signature value.",
   :label "Signature of Consenter"}}
     }}
)


(def sample-form

{:form
 {:collect-start "page1",
  :summary-start "revpage1",
  :header {:title "Consent For Medical Treatment"},
  :contains
  [{:name "page1",
    :title "Consent To Treat Me",
    :type "page",
    :contains
    [{:name "section1",
      :type "section",
      :contains
      [{:render-title true,
        :name "MedicalTreatmentText",
        :policy "P00001",
        :render-media true,
        :type "policy-text",
        :render-text true}
       {:name "MedicalTreatmentChoiceButtons",
        :policy "P00001",
        :type "policy-choice-buttons",
        :false-label "I do not agree",
        :true-label "I Agree"}]}
     {:name "section2",
      :type "section",
      :contains
      [{:text
        ["If either of these are not correct, please press the corresponding change button."],
        :name "ReferringDoctorText",
        :title "Release of Information to My Referring Doctor",
        :type "text"}]}
     {:name "section3",
      :type "section",
      :contains
      [{:meta-items ["MI002" "MI001"],
        :name "flagThemDoctors",
        :type "data-change"}]}],
    :next "page2"}
   {:name "page2",
    :title "Consent To Pay",
    :type "page",
    :contains
    [{:name "section1",
      :type "section",
      :contains
      [{:name "PaymentText", :policy "P00003", :type "policy-text"}
       {:name "PaymentChoiceButtons",
        :policy "P00003",
        :type "policy-choice-buttons",
        :false-label "I do not agree",
        :true-label "I Agree"}]}],
    :previous "page1",
    :next "page3"}
   {:name "page3",
    :title "Tissue Retention Disposal",
    :type "page",
    :contains
    [{:name "section1",
      :type "section",
      :contains
      [{:name "TissueText", :policy "P00004", :type "policy-text"}
       {:name "TissueCheckbox",
        :policy "P00004",
        :type "policy-checkbox",
        :unchecked-value true,
        :checked-value false,
        :label
        "I DO NOT agree to have my tissue or blood used for future research studies."}]}],
    :previous "page2",
    :next "page4"}
   {:name "page4",
    :title "Permission to Contact",
    :type "page",
    :contains
    [{:name "section1",
      :type "section",
      :contains
      [{:name "PermissionText", :policy "P00005", :type "policy-text"}
       {:name "PermissionCheckbox",
        :policy "P00005",
        :type "policy-checkbox",
        :unchecked-value true,
        :checked-value false,
        :label
        "I DO NOT agree to be contacted for future research studies."}]}],
    :previous "page3",
    :next "page5"}
   {:name "page5",
    :title "Photographs",
    :type "page",
    :contains
    [{:name "section1",
      :type "section",
      :contains
      [{:name "PhotographText", :policy "P00006", :type "policy-text"}
       {:name "PhotographChoiceButtons",
        :policy "P00006",
        :type "policy-choice-buttons",
        :false-label "I do not agree",
        :true-label "I Agree"}]}],
    :previous "page4",
    :next "page6"}
   {:name "page6",
    :title "PersonalItems",
    :type "page",
    :contains
    [{:name "section1",
      :type "section",
      :contains
      [{:name "PersonalItemsText",
        :policy "P00007",
        :type "policy-text"}
       {:name "PersonalItemsButton",
        :policy "P00007",
        :type "policy-button",
        :label "I Understand",
        :action-value true}]}],
    :previous "page5",
    :next "page7"}
   {:name "page7",
    :title "Copy of Privacy Practices",
    :type "page",
    :contains
    [{:name "section1",
      :type "section",
      :contains
      [{:name "PrivacyPracticesText",
        :policy "P00008",
        :type "policy-text"}
       {:name "PrivacyPracticesChoiceButtons",
        :policy "P00008",
        :type "policy-choice-buttons",
        :false-label "I do not agree",
        :true-label "I Agree"}]}],
    :previous "page6",
    :next "page8"}
   {:name "page8",
    :title "Consent Certifications",
    :type "page",
    :contains
    [{:name "section1",
      :type "section",
      :contains
      [{:render-title false,
        :name "ConsentText",
        :policy "P00009",
        :type "policy-text"}
       {:name "ConsentChoiceButtons",
        :policy "P00009",
        :type "policy-choice-buttons",
        :false-label "I do not agree",
        :true-label "I Agree"}]}
     {:name "section2",
      :type "section",
      :contains
      [{:endorsement "E00001",
        :name "consenter",
        :type "signature",
        :clear-label "Clear"}]}],
    :previous "page7",
    :next "page9"}
   {:name "revpage1",
    :title "Consent for Medical Treatment",
    :type "page",
    :contains
    [{:name "section1",
      :type "section",
      :contains
      [{:type "review-policy",
    	:name "ReviewConsentToTreat",
    	:title "Consent To Treat", 
    	:policy "P00001", 
    	:label "Edit",
	    :returnpage "page1"}]}
     {:name "section2",
      :type "section",
      :contains
      [{:type "review-metaitem",
   	    :name "ReviewMetaItem1",
    	:title "Review Information",         
        :meta-item "MI002",           
    	:label "Edit"}
	   {:type "review-metaitem",
   	    :name "ReviewMetaItem2",
    	:title "Review Information",         
        :meta-item "MI001",           
    	:label "Edit"}]}
     {:name "section3",
      :type "section",
      :contains
      [{:type "review-policy",
    	:name "ReviewConsentToPay",
    	:title "Consent To Pay For Services", 
    	:policy "P00003", 
    	:label "Edit",
	    :returnpage "page2"}
	   {:type "review-policy",
    	:name "ReviewTissueText",
    	:title "Tissue Donation For Research", 
    	:policy "P00004", 
    	:label "Edit",
	    :returnpage "page3"}]}],
	:next "revpage2"}
   {:name "revpage2",
    :title "Consent for Medical Treatment",
    :type "page",
    :contains
    [{:name "section1",
      :type "section",
      :contains
      [{:type "review-policy",
    	:name "ReviewPermissionToContact",
    	:title "Permission to be Contacted", 
    	:policy "P00005", 
    	:label "Edit",
	    :returnpage "page4"}
       {:type "review-policy",
    	:name "ReviewPermissionToPhoto",
    	:title "Permission to be Photographed", 
    	:policy "P00006", 
    	:label "Edit",
	    :returnpage "page5"}
	   {:type "review-policy",
    	:name "ReviewPersonalItems",
    	:title "Personal Items", 
    	:policy "P00007", 
    	:label "Edit",
	    :returnpage "page6"}
	   {:type "review-policy",
    	:name "ReviewPrivacyPractices",
    	:title "Privacy Practices", 
    	:policy "P00008", 
    	:label "Edit",
	    :returnpage "page7"}]}],
	:next "revpage3"}
   {:name "revpage3",
    :title "Consent for Medical Treatment",
    :type "page",
    :contains
    [{:name "section1",
      :type "section",
      :contains
      [{:type "review-policy",
    	:name "ReviewConsentCertifications",
    	:title "Consent Certifications", 
    	:policy "P00009", 
    	:label "Edit",
	    :returnpage "page8"}]}
     {:name "section2",
      :type "section",
      :contains
      [{:type "review-endorsement", 
    :name "ReviewEndorsement", 
    	:title "Review Signature",
    	:endorsement "E00001",
    	:label "Edit",
    	:returnpage "page8"}]}],
    :previous "revpage2"}],
  :footer {:title ["Patient Name: Bob R. Smith"]}}} 
  
  )


