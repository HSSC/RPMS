// INFO: We don't want jqm to keep the first page indefinitely, we need to break the cache, even if we are doing ajax
jQuery(document).bind("pagechange", function (toPage, info) {
    if (jQuery.mobile.firstPage && info.options.fromPage && (jQuery.mobile.firstPage == info.options.fromPage)) {
        jQuery.mobile.firstPage.remove();

        // We only need to remove 1 time from DOM, so unbind the unused event
        jQuery(document).unbind("pagechange", this);
    }
});
