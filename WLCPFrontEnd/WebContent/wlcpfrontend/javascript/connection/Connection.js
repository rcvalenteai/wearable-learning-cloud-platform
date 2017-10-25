/**
 * 
 */

class Connection {
	constructor(connectionFrom, connectionTo, gameConnectionId) {
		this.connectionFrom = connectionFrom;
		this.connectionTo = connectionTo;
		this.gameConnectionId = gameConnectionId;
	}
	
	static load() {
		var filters = [];
		filters.push(new sap.ui.model.Filter({path: "Game", operator: sap.ui.model.FilterOperator.EQ, value1: sap.ui.getCore().byId("gameEditor").getController().gameModel.GameId}));
		sap.ui.getCore().getModel("odata").read("/Connections", {filters: filters, success: $.proxy(this.loadSuccess, this), error: this.saveError});
	}
	
	static loadSuccess(oData) {
		for(var i = 0; i < oData.results.length; i++) {
			//sap.ui.getCore().byId("gameEditor").getController().jsPlumbInstance.connect({source: oData.results[i].ConnectionFrom, target: oData.results[i].ConnectionTo, anchors: ["Bottom", "Top"]});
			if(oData.results[i].ConnectionFrom == "start") {
				var ep1 = sap.ui.getCore().byId("gameEditor").getController().jsPlumbInstance.selectEndpoints({element : oData.results[i].ConnectionFrom}).get(0);
				var ep2 = sap.ui.getCore().byId("gameEditor").getController().jsPlumbInstance.selectEndpoints({element : oData.results[i].ConnectionTo}).get(0);
				sap.ui.getCore().byId("gameEditor").getController().jsPlumbInstance.connect({source: ep1 , target: ep2});
			} else {
				var ep1 = sap.ui.getCore().byId("gameEditor").getController().jsPlumbInstance.selectEndpoints({element : oData.results[i].ConnectionFrom}).get(1);
				var ep2 = sap.ui.getCore().byId("gameEditor").getController().jsPlumbInstance.selectEndpoints({element : oData.results[i].ConnectionTo}).get(0);
				sap.ui.getCore().byId("gameEditor").getController().jsPlumbInstance.connect({source: ep1 , target: ep2});
			}
		}
		sap.ui.getCore().byId("gameEditor").getController().busy.close();
		sap.m.MessageToast.show("Game Loaded Successfully!");
	}
	
	save() {
		var filters = [];
		filters.push(new sap.ui.model.Filter({path: "Game", operator: sap.ui.model.FilterOperator.EQ, value1: sap.ui.getCore().byId("gameEditor").getController().gameModel.GameId}));
		filters.push(new sap.ui.model.Filter({path: "GameConnectionId", operator: sap.ui.model.FilterOperator.EQ, value1: this.gameConnectionId}));
		
		//Read in the state data
		sap.ui.getCore().getModel("odata").read("/Connections", {filters: filters, success: $.proxy(this.saveConnection, this), error: this.saveError});
	}
	
	saveConnection(oData) {
		var saveData = {
			ConnectionId : 0,
			GameConnectionId : this.gameConnectionId,
			ConnectionFrom : this.connectionFrom,
			ConnectionTo : this.connectionTo,
			GameDetails : {
				__metadata : {
		             uri : "http://localhost:8080/WLCPWebApp/WLCPOData.svc/Games('" + sap.ui.getCore().byId("gameEditor").getController().gameModel.GameId + "')"
		         }
			}
		}
		
		if(oData.results.length == 1) {
			
			//We need to update the entry
			sap.ui.getCore().getModel("odata").update("/Connections(" + oData.results[0].ConnectionId + ")", saveData, {success: this.saveSuccess, error: this.saveError});
				
		} else if(oData.results.length == 0) {
			
			//We need to create the entry
			sap.ui.getCore().getModel("odata").create("/Connections", saveData, {success: this.saveSuccess, error: this.saveError});

		} else {
			//Something went terribly wrong...
		}
	}
	
	saveSuccess() {
		sap.ui.getCore().byId("gameEditor").getController().saveFSM();
	}
	
	saveError() {
		
	}
}