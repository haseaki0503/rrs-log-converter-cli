import * as fs from 'fs';
import * as path from 'path';
import * as readline from 'readline';
import * as child_process from "child_process";

import Logparser from './logparser';

import * as AWS from 'aws-sdk';
import { resolve } from 'dns';

async function run() {

  const key =
    await question("key[66cd4930-54cf-48aa-9a31-c508893ec6c7]: ");
  const teamname = await question("teamname[AIT]: ");
  const mapname = await question("mapname[Berlin]: ");
  const prefix = await question("prefix[rcap2021/test-logs/1/]: ");

  console.log('key=>', key, 'map=>', mapname, 'team=>', teamname, 'prefx=>', prefix);

  let rawZippedLogPath = path.join(process.cwd(), 'download/', key + '.zip');
  if (!fs.existsSync(path.join(process.cwd(), 'download/', key + '.zip'))) {
    rawZippedLogPath = await getZippedLog(key + '.zip');
  }

  let rawLogPath = path.join(process.cwd(), 'download/');
  if (!fs.existsSync(path.join(process.cwd(), 'download/', key))) {
    await extractZip(rawLogPath + key, rawLogPath);
  }

  let jsonLogDir =
    await parseLog(path.join('download/', key, 'SIMULATION/', 'rescue.log'), teamname, mapname);

  uploadLog(prefix, path.normalize(jsonLogDir), "viewer-json-logs1");

  console.log('Done.');

}

function question(query: string): Promise<string> {
  const rl = readline.createInterface({
    input: process.stdin,
    output: process.stdout
  });

  return new Promise((resolve) => {
    rl.question(query, (ans) => {
      resolve(ans);
      rl.close();
    });
  })

}

async function parseLog(logfilePath: string, teamname: string, mapname: string): Promise<string> {

  return new Promise(async (resolve) => {
    const logparser = new Logparser();
    const rowLogfilePath = path.resolve(logfilePath);
    const outputDirPath = fs.mkdtempSync(process.cwd() + '/');

    try {
      const convertedFile =
        await logparser.convertRescueLogToJSON(rowLogfilePath, outputDirPath + '/');

      const logpath = logparser.parse(convertedFile, './public/logs/', teamname, mapname);
      fs.rmdirSync(outputDirPath, { recursive: true });

      resolve(logpath);

    } catch (e) {
      console.log(e);
    }
  })
}

function getZippedLog(key: string): Promise<string> {

  return new Promise((resolve) => {

    const s3: AWS.S3 = new AWS.S3();

    const rawZippedLogPath = path.join(process.cwd(), 'download/', key);
    const getObjectParams: AWS.S3.GetObjectRequest = {
      Bucket: 'rrs-aws-s3bucket-1ukh3sl84bpi',
      Key: 'result/' + key
    }

    s3.getObject(getObjectParams, (err, data) => {
      if (err) {
        console.log(err)
      } else {
        const writer = fs.createWriteStream(rawZippedLogPath);
        writer.on("finish", () => {
          console.log("success", rawZippedLogPath);
        })
        writer.write(data.Body);
        writer.end('', () => {
          resolve(rawZippedLogPath);
        });
      }
    });

  });

}

function uploadLog(prefix: string, s3Path: string, bucketName: string) {

  let s3 = new AWS.S3();

  function walkSync(currentDirPath: string, callback: any) {
    fs.readdirSync(currentDirPath).forEach(function (name) {
      var filePath = path.join(currentDirPath, name);
      var stat = fs.statSync(filePath);
      if (stat.isFile()) {
        callback(filePath, stat);
      } else if (stat.isDirectory()) {
        walkSync(filePath, callback);
      }
    });
  }

  walkSync(s3Path, (filePath: any, stat: any) => {
    let bucketPath = path.join(prefix, filePath.substring(s3Path.length + 1));
    let params = { Bucket: bucketName, Key: bucketPath, Body: fs.readFileSync(filePath) };
    s3.putObject(params, function (err, data) {
      if (err) {
        console.log(err)
      } else {
        console.log('Successfully uploaded ' + bucketPath + ' to ' + bucketName);
      }
    });

  });
};

/**
 * Extract Zipfile
 * @param inFile string
 * @param outDir string?
 * @returns 
 */
function extractZip(inFile: string, outDir: string): Promise<string> {
  return new Promise((resolve) => {

    const proc = child_process.spawn(
      '/usr/bin/unzip',
      [
        inFile,
	'-d',
	outDir
      ]
    )

    proc.stderr.on('data', (stderr) => {
      let err = stderr.toString();
      console.error(err);
      throw err;
    });

    proc.stdout.on('data', (stdout) => {
      console.log(stdout.toString());
    });

    proc.on('exit', (code) => {
      resolve('');
    });

  })

}

run();
