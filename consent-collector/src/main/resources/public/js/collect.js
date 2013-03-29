(function( window, undefined ) {
	var consent = window.Consent;
	if(consent == null){
		consent = window.Consent = {};
	}
	
	var collect = consent.Collect;
	if(collect == null){
		collect = consent.Collect = {};
	}
	
	collect.session = {};
	collect.reset = function(){
		collect.session = {};
	};

	collect.location = function(){
		if(arguments.length > 0){
			collect._location = arguments[0];
			return collect;
		}
		else{
			return collect._location;
		}
	};
	
	collect.consenter = function(){
		if(arguments.length > 0){
			collect.session.consenter = arguments[0];
			return collect;
		}
		else{
			return collect.session.consenter;
		}
	};
	
	collect.encounter = function(){
		if(arguments.length > 0){
			collect.session.encounter = arguments[0];
			return collect;
		}
		else{
			return collect.session.encounter;
		}
	};
	
	collect.protocolIds = function(){
		if(arguments.length > 0){
			collect.session["protocol-ids"] = arguments[0];
			return collect;
		}
		else{
			return collect.session["protocol-ids"];
		}
	};
	
	collect.language = function(){
		if(arguments.length > 0){
			collect.session.language = arguments[0];
			return collect;
		}
		else{
			return collect.session.language;
		}
	};
	
	collect.defaultLanguage = function(){
		var loc = collect.location();
		if(loc.language != null){
			return loc.language;
		}
		if(loc.organization != null && loc.organization.language != null){
			return loc.organization.language;
		}
		return null;
	};
	
	collect.protocols = function(){
		if(arguments.length > 0){
			collect.session.protocols = arguments[0];
			collect.indexProtocols();
			return collect;
		}
		else{
			return collect.session.protocols;
		}
	};
	
	collect.indexProtocols = function(){
		if(collect.session.protocols != null){
			for(var i = 0; i < collect.session.protocols.length; i++){
				collect.session.protocols[i].index = i;
				collect.session.protocols[i].previous = collect.session.protocols.get(i - 1);
				collect.session.protocols[i].next = collect.session.protocols.get(i + 1);
			}
		}
	};
	
	collect.metaItem = function(id){
		return collect.session["meta-items"][id];
	};
	
	collect.metaItems = function(){
		if(arguments.length > 0){
			var mmap = {};
			var mord = [];
			$.each(arguments[0], function(i, mi){
				mi.index = i;
				mmap[mi.id] = mi;
				mord[i] = mi.id;
			});
			collect.session["meta-items"] = mmap;
			collect.session["meta-items-order"] = mord;
			return collect;
		}
		else{
			return collect.session["meta-items"];
		}
	};
		
	collect.orderedMetaItems = function(){
		var mord = collect.session["meta-items-order"];
		var items = [];
		$.each(collect.session["meta-items-order"], function(i, id){
			items[i] = collect.session["meta-items"][id];
		});
		return items;
	};
	
	collect.getData = function(){
		var protocols = collect.protocols();
		var consents = [];
		var endorsements = [];
		var metaitems = [];
		var encounter = {id: Consent.Collect.encounter().id};
		var organization = {id: Consent.Collect.location().organization.id};
		
		for(var i = 0; i < protocols.length; i++){
			var protocol = protocols[i];
			var common = {encounter: encounter, organization: organization, "protocol-version": {id: protocol.id}};
			for(pid in protocol.policies){
				consents.push($.extend({consented: protocol.policies[pid].value, policy: {id: pid}}, common));
			}
			for(eid in protocol.endorsements){
				endorsements.push($.extend({value: protocol.endorsements[eid].value, endorsement: {id: eid}}, common));
			}
			for(mid in protocol["meta-items"]){
				metaitems.push($.extend({value: collect.metaItem(mid).value, "meta-item": {id: mid}}, common));
			}
		}
		return {consents: consents, "consent-endorsements": endorsements, "consent-meta-items": metaitems};
	};
})( window );