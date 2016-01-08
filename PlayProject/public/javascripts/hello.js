if (window.console) {
  console.log("Welcome to your Play application's JavaScript!");
}

var app = angular.module("vapp",[]);

var Alert = function(type, text, active){
	this.type = type != undefined ? type : "info";
	this.text = text != undefined ? text : "";
	this.active = active != undefined ? active : true;
}
app.controller("MainController",function($scope){
	$scope.alert = new Alert(null,null,false);
	
	$scope.container = {};
	$scope.container.image = "";
	$scope.container.ports = [];
	
	$scope.creatingContainer = {}
	$scope.copyingVM = {};
	$scope.deleteVM = {};
	
	$scope.reload = function(){
		window.location.reload();
	}
	
	$scope.addPort = function(){
		if($scope.bind === parseInt($scope.bind, 10)){
			$scope.container.ports.push({"in" : $scope.bind, protocol : 'tcp', out : $scope.bind});
			$scope.bind = "";
		}
	}
	
		
	$scope.socket = function(route, object, active){
		active.value = true;
		console.log("open socket", object);
		//$scope.alert = new Alert("info","Creating VM")
		console.log(window.location.hostname)
		var socket = new WebSocket("ws://"+ window.location.hostname +"/"+route);
		socket.onopen = function (event) {
			console.log("socket opened")
			socket.send(JSON.stringify(object)); 
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
				active.value = false;
			}
			$scope.$digest();
		}
	}
	
})

//var exampleSocket = new WebSocket("ws://127.0.0.1:9000/socket");
