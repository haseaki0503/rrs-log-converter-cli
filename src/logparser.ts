import * as fs from 'fs';
import * as child_process from "child_process";
import { encode, decode } from '@msgpack/msgpack';

export default class Logparser {
  constructor() {
  }

  parse(jsonLogfile: any, outputPath: string, teamName: string, mapName: string) {

    const logfile: any = jsonLogfile;

    if (teamName == undefined) {
      teamName = 'No Team Name';
    }
    if (mapName == undefined) {
      let mapName = jsonLogfile.mapName;
    }

    const fileList = fs.readdirSync(outputPath, { withFileTypes: true });
    const logID = fileList.length <= 0 ? 1 : fileList.length + 1;

    const logpathbase: string = outputPath + logID;
    fs.mkdirSync(logpathbase + '/changed/', { recursive: true });
    fs.mkdirSync(logpathbase + '/full/', { recursive: true });

    var world;
    var scores: number[] = [];
    var agents: { [key: string]: any }[] = [];
    var buildings: { [key: string]: any }[] = [];
    var roads: { [key: string]: any }[] = [];
    var blockades: { [key: string]: any }[] = [];

    var noLinkedBlockeds = 0;

    for (var time = 1; time <= logfile.maxTimeStep; time++) {

      let fullLogPath: string = logpathbase + '/full/' + time + '.json';
      let changedLogPath: string = logpathbase + '/changed/' + time + '.json';

      let changedAgents: {}[] = [];
      let changedBuildings: {}[] = [];
      let changedRoads: {}[] = [];
      let changedBlockades: {}[] = [];

      var score: number = 0;
      var commands: [] = [];

      if (time === 1) {
        score = logfile.log[1].score;
        commands = [];

        const logWorld = logfile.log[1].world;

        // Full Log
        world = logfile.log[1].world;
        for (var i = 0; i < world.length; i++) {

          switch (world[i].type) {

            case "TacticsFire":
            case "TacticsPolice":
            case "TacticsAmbulance":
            case "Civilian":
              agents.push(world[i]);
              break;

            case "Building":
            case "ControlAmbulance":
            case "ControlFire":
            case "ControlPolice":
            case "GasStation":
            case "Refuge":
              buildings.push(world[i]);
              break;

            case "Road":
            case "Hydrant":
              roads.push(world[i]);
              break;

            case "Blockade":
              blockades.push(world[i]);
              break;

          }
        }

        // Changed Log
        changedAgents = agents;
        changedBuildings = buildings;
        changedRoads = roads;
        changedBlockades = blockades;

      }

      if (time !== 1) {

        score = logfile.log[time].score;
        commands = logfile.log[time].commands || [];

        var changes = logfile.log[time].changes;

        // Full Log
        changes.forEach((entity: { [x: string]: any; type?: any; id?: any; }) => {

          if (typeof entity.type !== "undefined") {

            // Add data
            switch (entity.type) {
              case "TacticsFire":
              case "TacticsPolice":
              case "TacticsAmbulance":
              case "Civilian":
                agents.push(entity);
                break;

              case "Building":
              case "ControlAmbulance":
              case "ControlFire":
              case "ControlPolice":
              case "GasStation":
              case "Refuge":
                buildings.push(entity);
                break;

              case "Road":
              case "Hydrant":
                roads.push(entity);
                break;

              case "Blockade":
                // BUG!
                blockades.push(entity);
                break;
            }

          } else {

            // Replace value
            var index = -1;
            index = agents.findIndex((v: { id?: number }) => v.id === entity.id);
            if (index > 0) {
              for (var key in entity) {
                agents[index][key] = entity[key];
              }
              return;
            }

            index = buildings.findIndex((v: { id?: number }) => v.id === entity.id);
            if (index > 0) {
              for (var key in entity) {
                buildings[index][key] = entity[key];
              }
              return;
            }

            index = roads.findIndex((v: { id?: number }) => v.id === entity.id);
            if (index > 0) {
              for (var key in entity) {
                roads[index][key] = entity[key];
              }
              return;
            }

            index = blockades.findIndex((v: { id?: number }) => v.id === entity.id);
            if (index > 0) {
              for (var key in entity) {
                blockades[index][key] = entity[key];
              }
              return;
            }

          }

        });

        // Changed Log
        changes.forEach((entity: { type?: any; id?: any; deleted?: boolean; }) => {

          if (typeof entity.type !== "undefined") {

            // Add data
            switch (entity.type) {
              case "TacticsFire":
              case "TacticsPolice":
              case "TacticsAmbulance":
              case "Civilian":
                changedAgents.push(entity);
                break;

              case "Building":
              case "ControlAmbulance":
              case "ControlFire":
              case "ControlPolice":
              case "GasStation":
              case "Refuge":
                changedBuildings.push(entity);
                break;

              case "Road":
              case "Hydrant":
                changedRoads.push(entity);
                break;

              case "Blockade":
                changedBlockades.push(entity);
                break;
            }

          } else {

            // Replace value
            var index = -1;
            index = fullLog.world.agents.findIndex((v: { id?: number }) => v.id === entity.id);
            if (index > 0) {
              changedAgents.push(fullLog.world.agents[index]);
              return;
            }

            index = fullLog.world.buildings.findIndex((v: { id?: number }) => v.id === entity.id);
            if (index > 0) {
              changedBuildings.push(fullLog.world.buildings[index]);
              return;
            }

            index = fullLog.world.roads.findIndex((v: { id?: number }) => v.id === entity.id);
            if (index > 0) {
              changedRoads.push(fullLog.world.roads[index]);
              return;
            }

            index = fullLog.world.blockades.findIndex((v: { id?: number }) => v.id === entity.id);
            if (index > 0) {
              changedBlockades.push(fullLog.world.blockades[index]);
              return;
            }
          }

        });

      }

      var fullLog = {
        'time': time,
        'score': score,
        'world': {
          'agents': agents,
          'buildings': buildings,
          'roads': roads,
          'blockades': blockades,
        },
        'commands': commands,
      }

      var changedLog = {
        'time': time,
        'score': score,
        'world': {
          'agents': changedAgents!,
          'buildings': changedBuildings!,
          'roads': changedRoads!,
          'blockades': changedBlockades!,
        },
        'commands': commands,
      }

      scores.push(score);

      console.log("");
      console.log("====================");
      console.log("time:", time);
      console.log("====================");
      console.log("Full Log:", agents.length + buildings.length + roads.length + blockades.length);
      console.log("agents:", agents.length);
      console.log("buildings:", buildings.length);
      console.log("roads:", roads.length);
      console.log("blockades:", blockades.length);
      console.log("commands:", commands.length);
      console.log("--------------------");
      console.log("Changed Log:", changedAgents!.length + changedBuildings!.length + changedRoads!.length + changedBlockades!.length);
      console.log("agents(changed):", changedAgents!.length);
      console.log("buildings(changed):", changedBuildings!.length);
      console.log("roads(changed):", changedRoads!.length);
      console.log("blockades(changed):", changedBlockades!.length);
      console.log("commands:", commands.length);
      console.log("====================");

      fs.writeFileSync(fullLogPath, JSON.stringify(fullLog));
      fs.writeFileSync(changedLogPath, JSON.stringify(changedLog));

    }

    console.log("No Linked Blockeds:", noLinkedBlockeds);
    console.log("");

    // map
    var path = logpathbase + '/map.json';
    var maplogjson = logfile.map
    fs.writeFileSync(path, JSON.stringify(maplogjson));
    console.log("map.json generated.");

    // meta
    var path = logpathbase + '/meta.json';
    var metajson = {
      'mapName': mapName,
      'teamName': teamName,
      'maxTimeStep': logfile.maxTimeStep,
      'scores': scores
    }
    fs.writeFileSync(path, JSON.stringify(metajson));
    console.log("meta.json generated.");

    console.log("");
    console.log("Log was generated at", logpathbase, "completely.");

    return logpathbase;

  }

  convertRescueLogToJSON(rowRescueLogfilePath: string, outputDirPath: string) {
    return new Promise((resolve) => {

      if (!fs.existsSync(outputDirPath)) {
        fs.mkdirSync(outputDirPath);
      }

      let proc = child_process.spawn('/usr/bin/Java',
        [
          '-jar',
          'viewermanager.jar',
          '-l',
          rowRescueLogfilePath,
          '-o',
          outputDirPath + 'rescuelog.json'
        ],
        {
          cwd: './lib/viewermanager/releases/ViewerManager/'
        }
      );

      let errorMessage = "";
      proc.stderr.on('data', (stderr) => {
        errorMessage = stderr.toString();
        console.error(errorMessage);
      });

      proc.stdout.on('data', (stdout) => {
        console.log(stdout.toString());
      });

      proc.on('exit', (code) => {

        if (!fs.existsSync(outputDirPath + 'rescuelog.json')) {
          throw new TypeError("Could not convert Rescue log to JSON. \n errorMessage");
        }

        const rescuelogJSON: Buffer =
          fs.readFileSync(outputDirPath + 'rescuelog.json');

        resolve(decode(rescuelogJSON));


      });

    });
  }

}
