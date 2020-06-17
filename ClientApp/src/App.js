import React from 'react';
import './App.css';
import axios from './axios';
import 'bootstrap/dist/css/bootstrap.min.css';
import 'bootstrap/dist/js/bootstrap.min.js';
import 'bootstrap/dist/js/bootstrap.bundle.min.js';
import './ProjectApp.css';
import 'font-awesome/css/font-awesome.min.css';
import moment from 'moment';

class App extends React.Component {

  state = {
    ipAddress: '',
    username: '',
    chatAppAddress: '',
    externalHost: '',
    messages: '',
    agentTypes: [],
    chosenAgentType: '',
    chosenHost: '',
    chosenAgentName: '',
    runningAgents: [],
    chosenJobType: '',
    chosenIndustry: '',
    chosenDegree: '',
    chosenYearsExperience: 0,
    chosenFinders: [],
    chosenPredictor: null,
    chosenMaster: null,
    chosenAgentToStopName: '',
    agentMessage: '',
    password: '',
    messageAll: '',
    messageUser: '',
    isSigningUp: false,
    isLoggedIn: false,
    loggedUser: '',
    users: [],
    onlineUsers: [],
    sendToUser: '',
    messages: [],
    subjectAll: '',
    subjectUser: '',
  }

  ws = null;

  componentDidMount() {
    this.setState({username: document.cookie.replace(/(?:(?:^|.*;\s*)usernameProjectApp\s*\=\s*([^;]*).*$)|^.*$/, "$1")});
    this.setState({chatAppAddress: document.cookie.replace(/(?:(?:^|.*;\s*)chatAppAddress\s*\=\s*([^;]*).*$)|^.*$/, "$1")});
    this.setState({externalHost: document.cookie.replace(/(?:(?:^|.*;\s*)externalHost\s*\=\s*([^;]*).*$)|^.*$/, "$1")});
    let username = document.cookie.replace(/(?:(?:^|.*;\s*)usernameProjectApp\s*\=\s*([^;]*).*$)|^.*$/, "$1");
    let chatAppAddress = document.cookie.replace(/(?:(?:^|.*;\s*)chatAppAddress\s*\=\s*([^;]*).*$)|^.*$/, "$1");
    let externalHost = document.cookie.replace(/(?:(?:^|.*;\s*)externalHost\s*\=\s*([^;]*).*$)|^.*$/, "$1");
    console.log(chatAppAddress);

    if(username !== null && username !== undefined && username !== "") {
      if (externalHost !== "" && externalHost !== undefined && externalHost !== null) {
        this.ws = new WebSocket("ws://" + externalHost + "ws/" + username);
        this.getAgentTypes(externalHost);
        this.getRunningAgents(externalHost);
        this.setState({ipAddress: externalHost});
      } else {
        console.log("Ovde usao");
        this.ws = new WebSocket("ws://" + chatAppAddress + "ws/" + username);
        this.getAgentTypes(chatAppAddress);
        this.getRunningAgents(chatAppAddress);
        this.setState({ipAddress: chatAppAddress});
      }

      this.ws.onopen = (evt) => {
        console.log('onopen: Socket Status: ' + this.ws.readyState + ' (open)');
      }

      this.ws.onmessage = (msg) => {
        var socketMessage = JSON.parse(msg.data);
        var msg = JSON.parse(socketMessage.message);
        var type = socketMessage.type;
        switch(type) {
          case "agentmessage":
            var newMessage = this.state.agentMessage + msg + "\n";
            this.setState({agentMessage: newMessage})
            break;
          case "runningagents":
            console.log(this.state.runningAgents);
            this.setState({runningAgents: msg});
            break;
          case "agenttypes":
            console.log(msg);
            console.log(this.state.agentTypes);
            break;
        }
        console.log('onmessage: Received: ' + msg); 
      }

      this.ws.onclose = (evt) => {
          console.log(evt);
          console.log("Session closed");
          this.ws = null;
      }
    }
  }

  getAgentTypes = (ip) => {
    axios.get("http://" + ip + "rest/agents/classes")
      .then(res => {
        let data = res.data;
        console.log(data);
        this.setState({agentTypes: data});
      }).catch(err => console.log("[ERROR] Unable to get agent types"));
  }

  getRunningAgents = (ip) => {
    axios.get("http://" + ip + "rest/agents/running")
      .then(res => {
        let data = res.data;
        console.log(data);
        this.setState({runningAgents: data});
      }).catch(err => console.log("[ERROR] Unable to get running agents"));
  }

  startAgent = (ip) => {
    if (ip.includes(this.state.chosenHost)) {
      axios.put("http://" + ip + "rest/agents/running/" + this.state.chosenAgentType + "/" + this.state.chosenAgentName)
      .then(res => console.log("[SUCCESS] Agent is added"))
      .catch(err => console.log(err.response.data));
    } else {
      console.log("Usao je sada ovde jer nisu isti");
      console.log(this.state.chosenHost);
      axios.post("http://" + ip + "rest/agents/running/" + this.state.chosenAgentType + "/" + this.state.chosenAgentName, this.state.chosenHost)
      .then(res => console.log("[SUCCESS] Agent from other host is added"))
      .catch(err => console.log(err.response.data));
    }
  }

  stopAgent = (ip) => {
    axios.delete("http://" + ip + "rest/agents/running/" + this.state.chosenAgentToStopName)
      .then(res => console.log("[SUCCESS] Agent is stopped"))
      .catch(err => console.log(err.response.data))
  }

  requestPredictionButton = (event) => {
    event.preventDefault();
    var userData = this.state.chosenJobType + ":" + this.state.chosenDegree + ":" + this.state.chosenIndustry + ":" + this.state.chosenYearsExperience;
    var ACLMessage = {
      "performative" : "STARTCOLLECTINGDATA",
      "sender" : this.state.chosenMaster,
      "receivers" : this.state.chosenFinders,
      "replyTo" : this.state.chosenPredictor,
      "conversationID" : "back",
      "numberOfPastReceivers" : this.state.chosenFinders.length,
      "replyWith" : userData
    }

    axios.post("http://" + this.state.ipAddress + "rest/messages", ACLMessage)
      .then(res => console.log("[SUCCESS] Message has been sent"))
      .catch(err => console.log(err.response))
    console.log(ACLMessage);
  }

  onBackButtonHandler = () => {
    window.location.replace("http://" + this.state.chatAppAddress + "home.html");
  }

  onSelected = (event) => {
    var at = event.target.value.split("-")[0].trim();
    var h = event.target.value.split("-")[1].trim();
    this.setState({chosenAgentType: at});
    this.setState({chosenHost: h});
  }

  onSelectedStop = (event) => {
    var atstop = event.target.value.split("-")[0].trim();
    this.setState({chosenAgentToStopName: atstop});
  }

  onStopAgentButton = (event) => {
    event.preventDefault();
    this.stopAgent(this.state.ipAddress);
  }

  onChangedInput = (event) => {
    this.setState({chosenAgentName: event.target.value});
  }

  onStartAgentButton = (event) => {
    event.preventDefault();
    this.startAgent(this.state.ipAddress);
  }

  onChangeJobType = (event) => {
    this.setState({chosenJobType: event.target.value});
  }

  onChangeDegree = (event) => {
    this.setState({chosenDegree: event.target.value});
  }

  onChangeIndustry = (event) => {
    this.setState({chosenIndustry: event.target.value});
  }

  onChangeYearsExperience = (event) => {
    this.setState({chosenYearsExperience: event.target.value});
  }

  onChangeFinders = (event) => {
    var options = event.target.options;
    var value = [];
    for (var i = 0, l = options.length; i < l; i++) {
      if (options[i].selected) {
        value.push(options[i].value.split("-")[0].trim());
      }
    }

    var agentIDs = [];
    for(var i = 0; i < value.length; i++) {
      for(var j = 0; j < this.state.runningAgents.length; j++) {
        if(value[i] === this.state.runningAgents[j].aid.name) {
          agentIDs.push(this.state.runningAgents[j].aid);
        }
      }
    }
    this.setState({chosenFinders: agentIDs});
  }

  onChangePredictor = (event) => {
    var agentName = event.target.value.split("-")[0].trim();
    var agentAID = null;
    for(var i = 0; i < this.state.runningAgents.length; i++) {
      if(agentName === this.state.runningAgents[i].aid.name) {
        agentAID = this.state.runningAgents[i].aid;
      }
    }

    this.setState({chosenPredictor: agentAID});
  }

  onChangeMaster = (event) => {
    var agentName = event.target.value.split("-")[0].trim();
    var agentAID = null;
    for(var i = 0; i < this.state.runningAgents.length; i++) {
      if(agentName === this.state.runningAgents[i].aid.name) {
        agentAID = this.state.runningAgents[i].aid;
      }
    }

    this.setState({chosenMaster: agentAID});
  }

  render() {
    return (
      <div className="container py-5">
        <div className="row mb-5">
          <div className="col-lg-8 text-white py-4 text-center mx-auto">
            <h1 className="display-4"><b>Salary Prediction</b></h1>
          </div>
        </div>

        <div className="p-5 bg-white rounded shadow mb-5">
          <ul id="myTab2" role="tablist" className="nav nav-tabs nav-pills with-arrow lined flex-column flex-sm-row text-center">
            <li className="nav-item flex-sm-fill">
              <a id="send-message-tab" data-toggle="tab" href="#send-message" role="tab" aria-controls="send-message" aria-selected="true" className="nav-link text-uppercase font-weight-bold mr-sm-3 rounded-0 active">Salary</a>
            </li>
          </ul>
          <div id="myTab2Content" className="tab-content">
            <div className="row" id="statusTextBoxId">
              <textarea className="form-control" id="exampleFormControlTextarea1" value={this.state.agentMessage} rows="3" disabled></textarea>
            </div>
            <div className="row" id="rowSalaryId">
              <div className="col-lg-6 mx-auto bg-white rounded shadow">
                <form>
                 <div className="form-group col-lg-12">
                      <label>Choose Job Type</label>
                      <select className="form-control" id="jobType" onChange={this.onChangeJobType} required>
                        <option disabled selected value> Select Job Type </option>
                        <option key="1">CTO</option>
                        <option key="2">MANAGER</option>
                        <option key="3">JUNIOR</option>
                        <option key="4">VICE_PRESIDENT</option>
                        <option key="5">JANITOR</option>
                        <option key="6">CEO</option>
                      </select>
                 </div>
                 <div className="form-group col-lg-12">
                      <label>Choose Degree</label>
                      <select className="form-control" id="degree" onChange={this.onChangeDegree} required>
                        <option disabled selected value> Select Degree </option>
                        <option key="0">BACHELORS</option>
                        <option key="1">DOCTORAL</option>
                        <option key="2">HIGH_SCHOOL</option>
                        <option key="3">MASTERS</option>
                        <option key="4">NONE</option>
                      </select>
                 </div>
                 <div className="form-group col-lg-12">
                      <label>Choose Industry</label>
                      <select className="form-control" id="industry" onChange={this.onChangeIndustry} required>
                        <option disabled selected value> Select Industry </option>
                        <option key="0">HEALTH</option>
                        <option key="1">WEB</option>
                        <option key="2">FINANCE</option>
                        <option key="3">EDUCATION</option>
                        <option key="4">OIL</option>
                      </select>
                 </div>
                 <div className="form-group col-lg-12">
                      <label>Enter years of experience</label>
                      <input class="form-control" type="text" placeholder="Years of experience" type="number" min="0" onChange={this.onChangeYearsExperience} required/>
                 </div>
                 <div className="form-group col-lg-12">
                      <label>Choose finder agents</label>
                      <select multiple className="form-control" onChange={this.onChangeFinders} id="finders" required>
                      {
                          this.state.runningAgents.map(function(item, i) {
                            if (item.aid.type.name === "FinderAgent") {
                              return <option key={i}>{item.aid.name}    -    {item.aid.type.name}    -   {item.aid.host.ipAddress}</option>;
                            }
                          })
                        }
                      </select>
                 </div>
                 <div className="form-group col-lg-12">
                      <label>Choose predictor</label>
                      <select className="form-control" id="predictor" onChange={this.onChangePredictor} required>
                        <option disabled selected value> Select Predictor Agent </option>
                      {
                          this.state.runningAgents.map(function(item, i) {
                            if (item.aid.type.name === "PredictorAgent") {
                              return <option key={i}>{item.aid.name}    -    {item.aid.type.name}    -   {item.aid.host.ipAddress}</option>;
                            }
                          })
                        }
                      </select>
                 </div>
                 <div className="form-group col-lg-12">
                      <label>Choose master agent</label>
                      <select className="form-control" id="master" onChange={this.onChangeMaster} required>
                        <option disabled selected value> Select Master Agent </option>
                      {
                          this.state.runningAgents.map(function(item, i) {
                            if (item.aid.type.name === "MasterAgent") {
                              return <option key={i}>{item.aid.name}    -    {item.aid.type.name}    -   {item.aid.host.ipAddress}</option>;
                            }
                          })
                        }
                      </select>
                 </div>
                 <button type="submit" className="btn btn-primary float-right btn-block mx-auto mb-4" onClick={this.requestPredictionButton} disabled={!this.state.chosenMaster} >Start prediction</button>
                </form>
              </div>
              <div className="col-lg-6 mx-auto bg-white rounded shadow">
                  <div className="table-responsive">
                      <table className="table table-fixed">
                          <thead>
                              <tr>
                                    <th scope="col" className="col-4">Run. agent</th>
                                    <th scope="col" className="col-4">Type</th>
                                    <th scope="col" className="col-4">Host</th>
                              </tr>
                          </thead>
                          <tbody id="registeredList">
                          {
                            this.state.runningAgents.map(function(item, i) {
                                return <tr><th scope="row" class="col-4"> {item.aid.name} </th><td class="col-4"> {item.aid.type.name}</td><td class="col-4">{item.aid.host.ipAddress} </td></tr>;
                            })
                          }
                          </tbody>
                      </table>
                  </div>
              </div>
            </div>
            <div className="row" id="statusTextBoxId">
            <div className="col-lg-12 mx-auto bg-white rounded shadow">
                <form>
                  <div className="form-row">
                    <div className="form-group col-lg-12">
                      <label>Choose Agent Type - Host</label>
                      <select className="form-control" id="exampleFormControlSelect2" onChange={this.onSelected} required>
                        <option disabled selected value> Select Agent Type - Host </option>
                        {
                          this.state.agentTypes.map(function(item, i) {
                            if (item.name !== "User") {
                              return <option key={i}>{item.name}    -    {item.module}</option>;
                            }
                          })
                        }
                      </select>
                    </div>
                  </div>

                  <div className="form-row">
                    <div className="form-group col-md-6">
                      <input type="text" className="form-control" onChange={this.onChangedInput} id="inputAgentName4" placeholder="Agent name" required/>
                    </div>
                    <div className="form-group col-md-6">
                      <button type="submit" className="btn btn-primary float-right btn-block" onClick={this.onStartAgentButton} disabled={!this.state.chosenAgentType || this.state.chosenAgentName.length === 0}>Start agent</button>
                    </div>
                  </div>
                </form>
              </div>
            </div>
            <div className="row" id="statusTextBoxId">
              <div className="col-lg-12 mx-auto bg-white rounded shadow">
                <form>
                  <div className="form-row">
                    <div className="form-group col-lg-12">
                      <label>Choose Agent To Stop</label>
                      <select className="form-control" id="exampleFormControlSelect2" onChange={this.onSelectedStop} required>
                        <option disabled selected value> Choose Agent To Stop </option>
                        {
                          this.state.runningAgents.map(function(item, i) {
                            if (item.aid.type.name !== "User") {
                              return <option key={i}>{item.aid.name}    -    {item.aid.type.name}    -   {item.aid.host.ipAddress}</option>;
                            }
                          })
                        }
                      </select>
                    </div>
                  </div>

                  <div className="form-row">
                    <div className="form-group col-md-12">
                      <button type="submit" className="btn btn-danger float-right btn-block" onClick={this.onStopAgentButton} disabled={!this.state.chosenAgentToStopName}>Stop agent</button>
                    </div>
                  </div>
                </form>
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  }

}

export default App;
