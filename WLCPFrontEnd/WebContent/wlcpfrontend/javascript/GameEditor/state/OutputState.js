/**
 * 
 */

class OutputState extends State {
	
	constructor(topColorClass, bottomColorClass, text, htmlId, jsPlumbInstance) {
		super(topColorClass, bottomColorClass, text, htmlId, jsPlumbInstance);
		this.inputEndPoint = {
				 endpoint:"Dot",
				 isTarget:true,
				 isSource:false,
				 maxConnections: -1
			};
		this.outputEndPoint = {
				 endpoint:"Dot",
				 isTarget:false,
				 isSource:true,
				 maxConnections: -1,
			};
		this.modelJSON = {
				iconTabs : []
		}
		this.modelJSON.iconTabs = this.generateData(3,3);
		this.model = new sap.ui.model.json.JSONModel(this.modelJSON);
		this.create();
	}
	
	create() {
		
		//Call the super method
		super.create();
		
		//Setup the end points
		this.jsPlumbInstance.addEndpoint(this.stateDiv.id, { id : this.htmlId + "input", anchor:"Top", paintStyle:{ fill: "#5E696E" } }, this.inputEndPoint);
		this.jsPlumbInstance.addEndpoint(this.stateDiv.id, { id : this.htmlId + "output", anchor:"Bottom", paintStyle:{ fill: "#5E696E" } }, this.outputEndPoint);
		
		//Setup double click
		$("#"+this.stateDiv.id).dblclick($.proxy(this.doubleClick, this));
	}
	
	doubleClick() {
		
		//Create an instance of the dialog
		this.dialog = sap.ui.xmlfragment("wlcpfrontend.fragments.GameEditor.States.OutputStateConfig", this);
		
		//Set the model for the dialog
		this.dialog.setModel(this.model);
			
		//Open the dialog
		this.dialog.open();
	}
	
	createData() {
		return {
			icon : "",
			scope : "",
			displayText : ""
		}
	}
	
	generateData(teams, playersPerTeam) {
		
		//Create a new object to store the data
		var baseData = [];

		//Add game wide
		var data = this.createData();
		data.icon = "sap-icon://globe";
		data.scope = "Game Wide";
		baseData.push(data);
		
		//Add the teams
		for(var i = 0; i < teams; i++) {
			data = this.createData();
			data.icon = "sap-icon://group";
			data.scope = "Team " + (i + 1);
			baseData.push(data);
		}
		
		//Add the players
		for(var i = 0; i < teams; i++) {
			for(var n = 0; n < playersPerTeam; n++) {
				data = this.createData();
				data.icon = "sap-icon://employee";
				data.scope = "Team " + (i + 1) + " Player " + (n + 1);
				baseData.push(data);
			}
		}
		
		return baseData;
	}
	
	closeDialog() {
		this.dialog.close();
		this.dialog.destroy();
	}
	
	navigationSelected(oEvent) {
		var key = oEvent.getParameter("item").getKey();
		var navContainer = oEvent.oSource.getParent().getParent().getContentAreas()[1];
		for(var i = 0; i < navContainer.getPages().length; i++) {
			if(navContainer.getPages()[i].getTitle().includes(key)) {
				navContainer.to(navContainer.getPages()[i]);
				break;
			}
		}
	}
	
	static load(loadData) {
		//Create a new display state
		var outputState = new OutputState("toolboxOutputStateTopColor", "toolboxOutputStateBottomColor", "Output State", loadData.stateId, GameEditor.getEditorController().jsPlumbInstance);
		
		//Set the position
		outputState.setPositionX(loadData.positionX); outputState.setPositionY(loadData.positionY);
		
		//Redraw it
		outputState.draw();
		
		//Push back the state
		GameEditor.getEditorController().stateList.push(outputState);
		
		//Load the states components
		outputState.loadComponents(loadData);
	}
	
	loadComponents(loadData) {
		for(var key in loadData.displayText) {
			for(var n = 0; n < this.modelJSON.iconTabs.length; n++) {
				if(key == this.modelJSON.iconTabs[n].scope) {
					this.modelJSON.iconTabs[n].displayText = loadData.displayText[key];
				}
			}
		}
	}
	
	save() {
		var outputStateData = {};
		for(var i = 0; i < this.modelJSON.iconTabs.length; i++) {
			if(this.modelJSON.iconTabs[i].displayText != "") {
				outputStateData[this.modelJSON.iconTabs[i].scope] = this.modelJSON.iconTabs[i].displayText;
			}
		}
		
		var saveData = {
			stateId : this.htmlId,
			positionX : this.positionX,
			positionY : this.positionY,
			stateType : "OUTPUT_STATE",
			displayText : outputStateData
		}
		
		return saveData;
	}
}