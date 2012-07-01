(ns org.healthsciencessc.rpms2.consent-collector.mock)

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
       {:meta-items [ "ee65929a-6aa8-4635-9034-99b1bb3efcae"
                      "5a1b8929-730a-4063-ae6f-08568139494a"
                      "e53f6862-2827-4893-aeb9-d18de39ff134" ], 
        :name "", 
        :type "data-change"}]}
     {:name "section2",
      :type "section",
      :contains
      [{:render-title false,
        :name "Acknowledge",
        :policy "P00001",
        :type "policy-text",
        :render-text true}
       {:type "media"
        :name "cat-video"
        :title "Awesome Cat Videos!"
        :properties [{:key "sources" :value ["http://html5demos.com/assets/dizzy.mp4"
                                             "http://shruwey.com/Demos/Videos/cat.mp4"
                                             "http://dherna.homeip.net/videopresentacion/media/rock_star_cat_acting_cute_online-convert_com.ogg"]}
                     {:key "posters" :value ["http://cvcl.mit.edu/hybrid/cat2.jpg"
                                             "http://mikecane.files.wordpress.com/2007/03/kitten.jpg"
                                             "http://25.media.tumblr.com/tumblr_lzaeceIRPD1r7fzdvo1_400.jpg"]}]}
       {:endorsement "E00001",
        :name "consenter",
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
        :meta-item "5a1b8929-730a-4063-ae6f-08568139494a",           
    	:label "Edit"}
	   {:type "review-metaitem",
   	    :name "ReviewMetaItem2",
    	    :title "Review Information",         
            :meta-item "ee65929a-6aa8-4635-9034-99b1bb3efcae"
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
        :name "consenter", 
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
      [{:meta-items [
                     "019f6f67-410c-40e2-99f0-52f3f5c8add1"
                     "5a1b8929-730a-4063-ae6f-08568139494a"
                     ],
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
        :meta-item "019f6f67-410c-40e2-99f0-52f3f5c8add1"
    	:label "Edit"}
	   {:type "review-metaitem",
   	    :name "ReviewMetaItem2",
    	:title "Review Information",         
        :meta-item "5a1b8929-730a-4063-ae6f-08568139494a"
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
    :name "consenter", 
    	:title "Review Signature",
    	:endorsement "E00001",
    	:label "Edit",
    	:returnpage "page8"}]}],
    :next "revpage4"
    :previous "revpage2"}

   {:name "revpage4",
    :title "Consent for Medical Treatment",
    :type "page",
    :contains
    [{:name "section1",
      :type "section",
      :contains
      [{:type "review-policy",
    	:name "ReviewPersonalItemsButton",
    	:title "PB Consent Certifications", 
        :policy "P00007",
    	:label "Edit",
        :returnpage "page6"}]}
     {:name "section2",
      :type "section",
      :contains
      [{:type "review-endorsement", 
    :name "consenter", 
    	:title "Review Signature",
    	:endorsement "E00001",
    	:label "Edit",
    	:returnpage "page8"}]}],
    :previous "revpage3"}
   
   ],
  :footer {:title ["Patient Name: Bob R. Smith"]}
  

 :meta-items
 {:MI002 {:label "Gaurantor", :value "Medical Services Foundation"},
  :MI001 {:label "Admission Date", :value "05-24-2012"}
  },


  :policies
  { :P00001 { :title "Your Permission Is Needed",
         :text ["I, Bob R. Smith, consent to and authorize medical treatment and diagnostic procedures which may be ordered by my doctors and performed by MUSC Medical Center (\"The Hospital\"). I consent to have blood drawn and to be tested for infectious diseases, including but not limited to: syphilis, AIDS, hepatitis, and testing for drugs if my doctor orders these tests."],
          :media  "http://obis-vac-stg.mdc.musc.edu/videos/bodpod.mp4" 
           } ,
    :P00003  {
       :title "Your Agreement to Pay for Treatment",
       :text [
"I assign and transfer to The Hospital and / or my doctors all rights, and interest in benefits I may have under any insurance policy I may have, including but not limited to hospitalization, medical, third party liability insurance coverage, workers compensation benefits, or benefits paid by Medicare or Medicaid. This assignment is intended to include any interest in benefits that I may have relating to this date of service as well as any prior dates of service. I direct that any insurance company or other party make payment of such benefits to The Hospital or my doctor. I authorize The Hospital and / or my doctor to collect benefits from any responsible third party through whatever means may be deemed necessary, and to endorse benefit checks made payable directly to me.", 

              "I understand that by signing below, I promise to pay all Hospital and doctor charges at the standard rates and terms of The Hospital or doctor including all charges not covered by my insurance or any other party. I promise to pay the patient's account at the rates stated in The Hospital's price list (known as the \"Charge Master\") and / or the doctor's fee schedule in effect on the date the charge is processed for the services provided. I understand that there will be a separate charge for the doctor and other professional services, but understand that The Hospital may bill for some professional fees.",
"I understand that The Hospital files insurance as a courtesy to me, and agree that I am responsible for payment of my bill, including any charges that are denied by my insurance or any other responsible party. I understand that care that is experimental as determined by my insurance company may not be covered and that I will be responsible for those charges. I agree that if this account is not paid, it may be turned over to a collection agency or attorney, and I must pay the amount due plus all costs of collection, including reasonable attorney's fees.", 

              "I understand that if I am unable to pay my bills, I may speak with a Financial Counselor to determine whether I qualify for assistance or for a discount. I may call (843) 792-2311 for information or questions about my hospital bill and (843) 792-6200 for information about my doctor bill."]
    },
   :P00004 {
:title "Retention / Disposal and Use of Blood, Body Fluids, or Tissue",
:text [ 
     "I understand and agree that any blood, body fluids or tissues normally removed from my body by MUSC in the course of any diagnostic procedures, surgery, or medical treatment that would otherwise be disposed of may be retained and used for research, including research on the genetic material (DNA) or other information contained in those tissues or specimens. I acknowledge that such research by MUSC may result in new inventions that may have commercial value and I understand that there are no plans to compensate me should this occur, regardless of the value of any such invention. I understand that any research using these leftover specimens or tissues will be done in a way that will not identify me.", 
"I also understand that if I do not want research to be done using my leftover blood, body fluids or tissue, I need to check the box shown below. If I have questions, I should call (843) 792-8300."]
	},

  :P00005 {
	:title "Permission to Contact for Research Studies",
	:text [
"I agree to be contacted about future research studies at MUSC for which I may be eligible. I understand that if I do not want to be contacted about future research studies, I need to check the box shown below."]
	},
  :P00006 {
	:title "Admission Photographs",
	:text [
"I agree that my photograph may be taken for purposes of identifying me, or providing treatment to me. This photograph may become part of my medical record and may be disclosed if copies of my medical record are disclosed."]
	},
   :P00007 {
	:title "Responsibility for Personal Items",
	:text [
"I understand that The Hospital is not responsible for valuable items which I bring with me. I understand it is my responsibility to send any valuable items (such as medications, money, jewelry, electronics, etc.) home for safe keeping. Any items left at the hospital in excess of 30 days will be disposed of."]
    },
  :P00008 {
	:title "I received a copy of the MUSC Notice of Privacy Practices.",
	:text ["I received a copy of the MUSC Notice of Privacy Practices."]
   },
   :P00009 {
	:title "Consent and Certifications",
	:text [
"I certify that I have read or have had read to me this consent and agree to its terms. I also certify that I am the patient, or am duly authorized by the patient, or am duly appointed to sign this agreement. I accept and understand its terms."]
    } 
   }  	
  }
 } 
  )
