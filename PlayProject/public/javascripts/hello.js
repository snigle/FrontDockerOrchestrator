if (window.console) {
  console.log("Welcome to your Play application's JavaScript!");
}

var app = angular.module("vapp",[]);

var Alert = function(type = "info", text = "", active = true){
	this.type = type;
	this.text = text;
	this.active = active;
}
app.controller("MainController",function($scope){
	$scope.alert = new Alert(null,null,false);
	
	$scope.createContainer = function(){
		$scope.creatingContainer = true;
		$scope.alert = new Alert("info","Creating container")
		var socket = new WebSocket("ws://127.0.0.1:9000/container/create");
		socket.onopen = function (event) {
			  socket.send('{"image" : "'+$scope.image+'"}'); 
		};
		socket.onmessage = function (event) {
			var data = JSON.parse(event.data);
			console.log("data received",data);
			if(data.error){
				$scope.alert = new Alert("danger",data.error);
			}
			else{
				$scope.alert = new Alert("success",data.info);
			}
			socket.close();
			$scope.creatingContainer = false;
			$scope.$digest();
		}
//		socket.onclose = function () {
//			  if(confirm("If you leave this page, you will be not notified when the current task will finish. Leave anyway ?")){
//				  socket.close();
//				  
//			  }
//		};
		
	}
	$scope.test = function(){
		console.log($scope.alert);
	}
	
	$scope.copieVM = function(){
		$scope.copyingVM = true;
		$scope.alert = new Alert("info","Creating VM")
		var socket = new WebSocket("ws://127.0.0.1:9000/copieVM");
		socket.onopen = function (event) {
			  socket.send('start'); 
		};
		socket.onmessage = function (event) {
			var data = JSON.parse(event.data);
			console.log("data received",data);
			if(data.error){
				$scope.alert = new Alert("danger",data.error);
			}
			else if(data.info){
				$scope.alert = new Alert("info",data.info);
			}
			else{
				$scope.alert = new Alert("success",data.success);
				socket.close();
				$scope.copyingVM = false;
			}
			$scope.$digest();
			
		}

		
	}
})

//var exampleSocket = new WebSocket("ws://127.0.0.1:9000/socket");
