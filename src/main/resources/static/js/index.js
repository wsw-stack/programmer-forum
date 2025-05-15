$(function(){
	$("#publishBtn").click(publish);
});

function publish() {
	$("#publishModal").modal("hide");
	// 获取标题和内容
	var title = $("#recipient-name").val();
	var content = $("#message-text").val();
	// 发送异步请求(POST)
	$.post(
	    CONTEXT_PATH + "/discuss/add",
	    {"title": title, "content": content},
	    function(data) {
	        data = $.parseJSON(data);
	        // 提示框中显示返回的消息
	        $("#hintBody").text(data.msg);
	        // 显示提示框，包含返回信息
	        $("#hintModal").modal("show");
            	setTimeout(function(){
            		$("#hintModal").modal("hide");
            		// 刷新
            		if(data.code == 0) {
            		    window.location.reload();
            		}
            	}, 2000);
	    }
	)
}