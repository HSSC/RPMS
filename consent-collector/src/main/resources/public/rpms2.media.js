var rpms_media = {
    
    makeVideosPages: function(){
        var videoPages = $(".video-pages");
        var mainPage = $("#one");
        videoPages.remove();
        mainPage.after(videoPages);
    },

    setCloseButton: function(dialog){
        $(dialog).find("a[title='Close']").attr("href", "#one");
    },

    isVideoPage: function(pageData){
        return pageData.hasClass("video-pages");
    }
}

$(document).on('pageinit', rpms_media.makeVideosPages);
$(document).on("pagechange", function(e, data){
    var page = data.toPage;
    if(rpms_media.isVideoPage(page)){
        rpms_media.setCloseButton(page);
    }

    var videos = $("video")
    $.each(videos, function(index, video){
        video.pause();
    });
});
