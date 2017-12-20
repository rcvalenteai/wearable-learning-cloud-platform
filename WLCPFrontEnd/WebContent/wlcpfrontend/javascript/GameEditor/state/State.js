/**
 * 
 */

var StateType = {
		START_STATE : "START_STATE",
		OUTPUT_STATE : "OUTPUT_STATE"
}

class State {
	
	constructor(topColorClass, bottomColorClass, text, htmlId, jsPlumbInstance) {
		this.topColorClass = topColorClass;
		this.bottomColorClass = bottomColorClass;
		this.text = text;
		this.positionX = 0;
		this.positionY = 0;
		this.width = 0;
		this.height = 0;
		this.jsPlumbInstance = jsPlumbInstance;
		this.htmlId = htmlId;
		this.stateDiv = null;
		this.inputConnections = [];
		this.outputConnections = [];
	}
	
	absoluteToRelativeX(absoluteX) {
		return absoluteX - (document.getElementById("gameEditor--toolbox").getBoundingClientRect().width + document.getElementById("gameEditor--mainSplitter-splitbar-0").getBoundingClientRect().width + (this.width / 2));
	};
	
	absoluteToRelativeY(absoluteY) {
		return absoluteY + document.getElementById("gameEditor--toolbox-scroll").offsetHeight - 20;
	};
	
	create() {
		//Main div
		this.stateDiv = document.createElement('div');
		this.stateDiv.id = this.htmlId;
		this.stateDiv.className = "state stateBorderShadow jtk-drag-select";
		
		//Top color
		var topColorDiv = document.createElement('div');
		topColorDiv.className = this.topColorClass;
		
		//Top Color Text
		var topColorText = document.createElement('div');
		topColorText.className = "centerStateText";
		topColorText.innerHTML = this.text;
		topColorDiv.appendChild(topColorText);
		
		if(this.stateDiv.id != "start") {
			//Delete Button
			var deleteButton = document.createElement('div');
			deleteButton.id = this.htmlId + "delete";
			deleteButton.className = "close-thik";
			topColorDiv.appendChild(deleteButton);	
		}
		
		//Bottom color
		var bottomColorDiv = document.createElement('div');
		bottomColorDiv.className = this.bottomColorClass;
		
		//Append
		this.stateDiv.appendChild(topColorDiv);
		this.stateDiv.appendChild(bottomColorDiv);
		
		//Add the div to the pad
		document.getElementById('gameEditor--pad').appendChild(this.stateDiv);
		
		//Get the width and height
		this.width = this.stateDiv.getBoundingClientRect().width;
		this.height = this.stateDiv.getBoundingClientRect().height;
		
		//Make it draggable
		this.jsPlumbInstance.draggable(this.stateDiv.id, {containment : true, drag : $.proxy(this.moved, this), stop : $.proxy(this.moved, this)});
		//$("#" + this.stateDiv.id).draggable({containment : "parent", stop : $.proxy(this.moved, this)});
		
		if(this.stateDiv.id != "start") {
			//Make delete clickable
			$("#" + deleteButton.id).click($.proxy(this.remove, this));
		}
	}
	
	remove() {
		console.log("clcik");
	}
	
	draw() {

		//Set the start position
		document.getElementById(this.stateDiv.id).style.left = this.positionX + "px";
		document.getElementById(this.stateDiv.id).style.top = this.positionY + "px";
		
		//Repaint
		this.jsPlumbInstance.revalidate(this.stateDiv.id);
	}
	
	moved() {
		
		//Update the position
		this.positionX = parseFloat(document.getElementById(this.htmlId).style.left.replace("px", ""));
		this.positionY = parseFloat(document.getElementById(this.htmlId).style.top.replace("px", ""));
		
		if((this.positionX + this.width + 50) >= document.getElementById("gameEditor--pad").getBoundingClientRect().width) {
			document.getElementById("gameEditor--pad").style.width = (document.getElementById("gameEditor--pad").getBoundingClientRect().width + 500) + "px";
		}
		if((this.positionY + this.height + 50) >= document.getElementById("gameEditor--pad").getBoundingClientRect().height) {
			document.getElementById("gameEditor--pad").style.height = (document.getElementById("gameEditor--pad").getBoundingClientRect().height + 500) + "px";
		}
	}
	
	save() {
		return [];
	}
	
	getPositionX() {
		return this.positionX;
	}
	
	getPositionY() {
		return this.positionY;
	}
	
	setPositionX(positionX) {
		this.positionX = parseFloat(positionX);
	}
	
	setPositionY(positionY) {
		this.positionY = parseFloat(positionY);
	}
	
	getHtmlId() {
		return this.htmlId;
	}
}