// import firebase from "firebase/app";
const { onRequest, onCall } = require("firebase-functions/v2/https");
const { logger } = require("firebase-functions/v2");
const { getDatabase } = require("firebase-admin/database");
const { initializeApp } = require("firebase-admin/app");
const { Timestamp } = require("firebase-admin/firestore");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();

exports.getAdmin = onCall(async (req) => {
    try {
        let admin = await getDatabase().ref("/admin").get();
        admin = admin.val();
        return { admin };
    } catch (error) {
        logger.debug("Exception in getAdmin():" + error.stack);
        return error;
    }
});

async function getRankingsUtil() {
    let rankings = [];
    try {
        let teams = await getDatabase().ref("/teams").get();

        let masterData = await getDatabase().ref("/master").get();

        let currentTime = new Date();
        let est = new Date(masterData.child("eventStartTime").val());
        let qri = masterData.child("questionReleaseInterval").val() * 60 * 1000;
        let maxQuestionReleasePerStage = masterData
            .child("maxQuestionReleasePerStage")
            .val();

        let totalIntervalFromEventStart = currentTime - est;
        let eventCurrentStage = Math.floor(totalIntervalFromEventStart / qri);
        let totalStages = Math.floor(
            masterSnapshotData.child("questionDetails").numChildren() /
                maxQuestionReleasePerStage
        );
        eventCurrentStage = Math.min(eventCurrentStage, totalStages - 1);

        //If last stage keep overall standings shush
        // if (eventCurrentStage == totalStages - 1) return rankings;

        let eventCurrentLevel =
            1 + eventCurrentStage * maxQuestionReleasePerStage;

        let baseCumulativeScore = masterData
            .child("baseCumulativeScore")
            .child(eventCurrentLevel - 1 + "")
            .val();


        teams.forEach((team) => {
            let teamCode = team.key;
            let teamName = team.child("teamName").val();
            let score = team.child("score").val();
            let teamScore = score.teamScore + score.negativeScoreBalancer;
            let teamCurrentLevel = team.child("currentLevel").val();

            if (teamCurrentLevel > eventCurrentLevel) {
                teamScore += masterData
                    .child("baseCumulativeScore")
                    .child(teamCurrentLevel - 1 + "")
                    .val();
            } else teamScore += baseCumulativeScore;

            rankings.push({ teamScore, teamName, teamCode });
        });

        rankings.sort((a, b) => b.teamScore - a.teamScore);

        return rankings;
    } catch (error) {
        logger.debug("Exception in getRankingsUtil():" + error.stack);
        return error;
    }
}

exports.getRankings = onCall(async (req) => {
    let rankings = await getRankingsUtil();
    return { rankings };
});

async function isValidLevel(level, teamCode, masterSnapshotData) {
    try {
        let teamSnapshotData = await getDatabase()
            .ref(`/teams/${teamCode}`)
            .get();

        let currentTime = new Date();
        let est = new Date(masterSnapshotData.child("eventStartTime").val());
        let qri =
            masterSnapshotData.child("questionReleaseInterval").val() *
            60 *
            1000;
        let maxQuestionReleasePerStage = masterSnapshotData
            .child("maxQuestionReleasePerStage")
            .val();

        let totalIntervalFromEventStart = currentTime - est;
        let eventCurrentStage = Math.floor(totalIntervalFromEventStart / qri);
        //this will freeze last stage till event ends (we reached here because event is running)
        let totalStages = Math.floor(
            masterSnapshotData.child("questionDetails").numChildren() /
                maxQuestionReleasePerStage
        );
        eventCurrentStage = Math.min(eventCurrentStage, totalStages - 1);
        logger.debug(
            "Get Question: : eventCurrentStage" +
                eventCurrentStage +
                ", totalStages" +
                totalStages
        );
        let eventCurrentLevel =
            1 + eventCurrentStage * maxQuestionReleasePerStage;

        let teamCurrentLevel = teamSnapshotData.child("currentLevel").val();

        if (
            level == teamCurrentLevel &&
            level >= eventCurrentLevel &&
            level < eventCurrentLevel + maxQuestionReleasePerStage
        )
            return true;
        return false;
    } catch (error) {
        logger.debug("Exception in isValidLevel():" + error.stack);
        return error;
    }
}

function getCurrentScore(masterSnapshotData, teamSnapshotData) {
    try {
        let teamScore = teamSnapshotData
            .child("score")
            .child("teamScore")
            .val();
        let negativeScoreBalancer = teamSnapshotData
            .child("score")
            .child("negativeScoreBalancer")
            .val();
        // logger.debug("getCurrentScore():"+teamSnapshotData.child("currentLevel").val());
        let baseCumulativeScore = masterSnapshotData
            .child("baseCumulativeScore")
            .child(teamSnapshotData.child("currentLevel").val() - 1 + "")
            .val();

        teamScore = baseCumulativeScore + teamScore + negativeScoreBalancer;
        return teamScore;
    } catch (error) {
        logger.debug("Exception in getCurrentScore():" + error.stack);
        return error;
    }
}

exports.unlockHint = onCall(async (req) => {
    //Codes: EventNotStarted, EventEnded, EndGame, EventBlocked, UnlockedAllQuestions, unlockHint(): HintsExhausted
    let hint, maxAllowed, totalUsed;
    let masterSnapshotData, teamSnapshotData;
    let scoreRules;
    let teamScore, negativeScoreBalancer, baseCumulativeScore;
    let hintUsed;
    try {
        // Read all master data
        await getDatabase()
            .ref("/master")
            .get()
            .then((masterSnapshot) => {
                masterSnapshotData = masterSnapshot;
                maxAllowed = masterSnapshotData.child("maxHintsAllowed").val();
            });
        logger.debug(
            "masterSnapshotData: " + JSON.stringify(masterSnapshotData)
        );

        let eventState = getEventStateUtil(masterSnapshotData);
        if (eventState != null) return eventState;

        // Read all team data
        await getDatabase()
            .ref(`/teams/${req.data.teamCode}`)
            .get()
            .then((teamSnapshot) => {
                teamSnapshotData = teamSnapshot;
                totalUsed = teamSnapshotData.child("totalHintsUsed").val();
            });
        logger.debug("teamSnapshotData: " + JSON.stringify(teamSnapshotData));

        teamScore = getCurrentScore(masterSnapshotData, teamSnapshotData);

        let returnVal = {
            hint: null,
            totalHints: maxAllowed,
            availableHints: maxAllowed - totalUsed,
            teamScore: teamScore,
        };

        hint = masterSnapshotData
            .child("questionDetails")
            .child(req.data.level)
            .child("hint")
            .val();
        hintUsed = teamSnapshotData
            .child("scoreCard")
            .child(`${req.data.level}`)
            .child("hintUsed")
            .val();
        if (hintUsed) {
            returnVal.hint = hint;
            return returnVal;
        }

        // Check if hints are available or not
        if (totalUsed >= maxAllowed) {
            logger.debug("unlockHint(): " + JSON.stringify(returnVal));
            return returnVal;
        }

        // Update/create scorecard with hintUsed entry
        await getDatabase()
            .ref(`/teams/${req.data.teamCode}/scoreCard`)
            .update({
                [req.data.level]: {
                    hintUsed: true,
                    isSuccess: false,
                    time: new Date().toISOString(),
                    deviceId: req.data.deviceId,
                },
            });

        // Increment Hints used
        totalUsed = totalUsed + 1;
        await getDatabase()
            .ref(`/teams/${req.data.teamCode}`)
            .update({ totalHintsUsed: totalUsed });
        returnVal.availableHints = maxAllowed - totalUsed;

        returnVal.hint = hint;
        scoreRules = masterSnapshotData.child("scoreRules").val();
        teamScore -= scoreRules.hintScale * req.data.level;
        returnVal.teamScore = teamScore;

        await getDatabase().ref(`/teams/${req.data.teamCode}/score`).update({
            teamScore: teamScore,
        });

        //Will notify about unlocked hint to all the fellow teammates
        /*await getMessaging().send({
            data: {
                reload: true
            },
            topic: req.data.teamCode
        });*/

        return returnVal;
    } catch (error) {
        logger.debug("Exception in unlockHint():" + error.stack);
        return error;
    }
});

exports.checkAnswer = onCall(async (req) => {
    try {
        let level = req.data.level;
        let ans = req.data.ans;
        let teamCode = req.data.teamCode;
        let deviceId = req.data.deviceId;
        let realAns = null;
        let isSuccess = false;
        let scoreRules = null;
        let score = null;
        let earlyBird = false;
        let time = new Date().toISOString();
        let masterSnapshotData;
        let currentTeamScore;
        let teamSnapshotData;

        logger.debug("data received");
        logger.debug(req.data);

        await getDatabase().ref("/master").get().then(async (masterSnapshot) => {
            masterSnapshotData = masterSnapshot;
        });

        // Read all team data
        await getDatabase().ref(`/teams/${teamCode}`).get().then((teamSnapshot) => {
            teamSnapshotData = teamSnapshot;
        });

        currentTeamScore = getCurrentScore(masterSnapshotData, teamSnapshotData);

        let eventState = getEventStateUtil(masterSnapshotData);
        if (eventState != null) {
            eventState.teamScore = currentTeamScore;
            return eventState;
        }

        let validLevel = await isValidLevel(level, teamCode, masterSnapshotData);

        if (validLevel == false) {
            return {
                code: "InvalidLevel",
                msg: "Someone in your team already answered or time for the current level is over.",
                teamScore: currentTeamScore,
                time: null,
            };
        }

        scoreRules = masterSnapshotData.child("scoreRules").val();
        score = teamSnapshotData.child("score").val();

        let queSnapshot = masterSnapshotData.child("questionDetails").child(`${level}`);
        realAns = queSnapshot.child("answer").val().toLowerCase();


        // if (queSnapshot.child("earlyBird").child("answered").val() == false) {
        //     earlyBird = true;
        // }

        if (realAns === ans.toLowerCase()) {
            logger.debug("Answer correct");

            // If early bird then save detailes master data of question
            await getDatabase().ref(`/master/questionDetails/${level}/earlyBird`).transaction((earlyBirdData) => {
                if (earlyBirdData && earlyBirdData.answered === true) {
                    return earlyBirdData;
                }
                return {
                    answered: true,
                    teamCode,
                    deviceId,
                    time,
                };
            }, (error, commited, snapshot) => {
                if (error){
                    logger.debug("Exception in checkAnswer(): " + error.stack);
                    return error;
                }
                else if (commited && snapshot.val().teamCode === teamCode){
                    logger.debug("data commited");
                    earlyBird = true;
                }
            });

            // if (earlyBird) {
            //     await getDatabase()
            //         .ref(`/master/questionDetails/${level}/earlyBird`)
            //         .update({
            //             answered: true,
            //             teamCode: teamCode,
            //             deviceId: deviceId,
            //             time: time,
            //         });
            // }

            // Update team score as per hint and early bird
            await getDatabase().ref(`/teams/${teamCode}`).transaction((teamData) => {
                if (teamData && teamData.currentLevel > level){
                    return teamData;
                }
                if (teamData === null) return {};
                let teamScore = scoreRules.successfulScale * level;
                let negativeScoreBalancer = scoreRules.unsuccessfulScale * level;
                let teamDataNew = teamData;

                teamDataNew.currentLevel = level + 1;
                teamDataNew.scoreCard = {
                    [level]: {
                        hintUsed: false,
                        isSuccess: true,
                        time: time,
                        deviceId: deviceId,
                    }
                };

                if (earlyBird) {
                    if (scoreRules.earlyBirdScore != 0)
                        teamScore += scoreRules.earlyBirdScore;
                    else teamScore += scoreRules.earlyBirdScale * level;
                }
                teamDataNew.score = {
                    teamScore: score.teamScore + teamScore,
                    negativeScoreBalancer: score.negativeScoreBalancer + negativeScoreBalancer,
                };
                
                return teamDataNew;
            });

            // await getDatabase()
            //     .ref(`/teams/${teamCode}/scoreCard/${level}`)
            //     .get()
            //     .then(async (scoreCardSnapshot) => {
            //         let hintUsed = false;
            //         let teamScore = scoreRules.successfulScale * level;
            //         let negativeScoreBalancer =
            //             scoreRules.unsuccessfulScale * level;
            //         // To get old value to hint used in existing score card if any
            //         //done
            //         if (scoreCardSnapshot.exists()) {
            //             logger.debug(
            //                 "scoreCard exists: " +
            //                     JSON.stringify(scoreCardSnapshot)
            //             );
            //             hintUsed = scoreCardSnapshot.val().hintUsed;
            //             // if (hintUsed) {
            //             //     // teamScore -= scoreRules.hintScale * level;
            //             // }
            //         } else {
            //             logger.debug("scorecard does not exist: creating one");
            //         }

            //         //done
            //         if (earlyBird) {
            //             if (scoreRules.earlyBirdScore != 0)
            //                 teamScore += scoreRules.earlyBirdScore;
            //             else teamScore += scoreRules.earlyBirdScale * level;
            //         }

            //         // Update question score card in team
            //         //done
            //         await getDatabase()
            //             .ref(`/teams/${teamCode}/scoreCard/${level}`)
            //             .update({
            //                 hintUsed: hintUsed,
            //                 isSuccess: true,
            //                 time: time,
            //                 deviceId: deviceId,
            //             });

            //         // Update total team score and negative score balancer
            //         //done
            //         await getDatabase()
            //             .ref(`/teams/${teamCode}/score`)
            //             .update({
            //                 teamScore: score.teamScore + teamScore,
            //                 negativeScoreBalancer:
            //                     score.negativeScoreBalancer +
            //                     negativeScoreBalancer,
            //             });

            //         // Update team level to next question
            //         //done
            //         await getDatabase()
            //             .ref(`/teams/${teamCode}`)
            //             .update({
            //                 currentLevel: level + 1,
            //             });
            //     });
            isSuccess = true;
        }
        logger.debug("before calling get question function.");
        let newQuestion = await getQuestionFunction(teamCode, deviceId);
        newQuestion.isSuccess = isSuccess;
        newQuestion.earlyBird = earlyBird;
        logger.debug(
            "Updated question with Answer msg: " + JSON.stringify(newQuestion)
        );

        //Will notify fellow team mates about the successful submissionx
        // await getMessaging().send({
        //     data: {
        //         reload: true
        //     },
        //     topic: teamCode
        // });

        return newQuestion;
    } catch (error) {
        logger.debug("Exception in checkAnswer(): " + error.stack);
        return error;
    }
});

function getDateObjectFromTime(timestring, currentTime) {
    let hours = parseInt(timestring.slice(0, 2), 10);
    let minutes = parseInt(timestring.slice(2), 10);

    if (hours < 0 || hours > 23 || minutes < 0 || minutes > 59) {
        throw new Error(
            "Invalid time in hours or minutes passed in BlockInterval: " +
                timestring
        );
    }

    hours = (24 + hours - 5) % 24;
    minutes -= 30;
    if (minutes < 0) hours = (24 + hours - 1) % 24;
    minutes = (60 + minutes) % 60;

    const date = new Date(
        currentTime.getFullYear(),
        currentTime.getMonth(),
        currentTime.getDate(),
        hours,
        minutes,
        0,
        0
    );

    // date.setTime(date.getTime() - 19800 * 1000); //5:30hrs = 19800s * 1000ms

    return date;
}

function getEventStateUtil(masterSnapshotData) {
    //Codes: EventNotStarted, EventEnded, EndGame, EventBlocked, UnlockedAllQuestions, unlockHint(): HintsExhausted, getEventStatus(): EventRunning
    try {
        let currentTime = new Date();
        let est = new Date(masterSnapshotData.child("eventStartTime").val()); //est = event start time
        let eet = new Date(masterSnapshotData.child("eventEndTime").val()); //eet = event end time,
        let availableTime;
        // logger.debug("Temp 2");
        // Check if event started
        if (currentTime < est) {
            let customCode = "EventNotStarted";
            let userMsg = "Event not started.";
            logger.debug("Event Not Started");
            return {
                code: customCode,
                msg: userMsg,
                time: est - currentTime,
                score: 0,
            };
        }
        // logger.debug("Temp 3");
        // Check if event ended
        if (currentTime >= eet) {
            let customCode = "EventEnded";
            let userMsg = "Event Finished.";
            logger.debug("Event Ended");
            return {
                code: customCode,
                msg: userMsg,
                time: null,
            };
        }
        // logger.debug("Temp 4");
        // Check if event is blocked or paused
        let intervalsSnapshot = masterSnapshotData.child("blockIntervals");
        logger.debug(
            "Getting intervalsSnapshot: " + JSON.stringify(intervalsSnapshot)
        );
        logger.debug("this is to check wheter deploued");
        let noOfIntervals =
            intervalsSnapshot == null ? 0 : intervalsSnapshot.numChildren();
        logger.debug("Number of Block Intervals: " + noOfIntervals);
        for (let i = 0; i < noOfIntervals; i++) {
            let startTime = getDateObjectFromTime(
                intervalsSnapshot.child(i).child("start").val(),
                currentTime
            );
            let endTime = getDateObjectFromTime(
                intervalsSnapshot.child(i).child("end").val(),
                currentTime
            );
            logger.debug(
                "getDateObjectFromTime[" +
                    i +
                    "]: current:" +
                    currentTime +
                    " start:" +
                    startTime +
                    ", end:" +
                    endTime
            );
            if (currentTime >= startTime && currentTime <= endTime) {
                let customCode = "EventBlocked";
                let userMsg = "Event is paused.";
                let returnValue = {
                    code: customCode,
                    msg: userMsg,
                    startTime: startTime,
                    endTime: endTime,
                    time: endTime - currentTime,
                };
                logger.debug(
                    "Event is Blocked for this period: " +
                        JSON.stringify(returnValue)
                );
                return returnValue;
            }
        }
        return null; // if everything is fine then return null
    } catch (error) {
        logger.debug("Exception is getEventStateUtil(): " + error.stack);
        return error;
    }
}

exports.getEventState = onCall(async (req) => {
    //Codes: EventNotStarted, EventEnded, EndGame, EventBlocked, UnlockedAllQuestions, unlockHint(): HintsExhausted, getEventStatus(): EventRunning
    try {
        let masterSnapshotData, teamSnapshotData;
        let currentTeamScore;

        await getDatabase()
            .ref("/master")
            .get()
            .then(async (masterSnapshot) => {
                masterSnapshotData = masterSnapshot;
            });

        // Read all team data
        await getDatabase()
            .ref(`/teams/${req.data.teamCode}`)
            .get()
            .then((teamSnapshot) => {
                teamSnapshotData = teamSnapshot;
            });

        currentTeamScore = getCurrentScore(
            masterSnapshotData,
            teamSnapshotData
        );

        let eventState = getEventStateUtil(masterSnapshotData);
        logger.debug("the value came for event state " + eventState);
        if (eventState != null) {
            eventState.teamScore = currentTeamScore;
            return eventState;
        }
        
        return {
            code: "EventRunning",
            msg: "Event is on",
        };
    } catch (error) {
        logger.debug("Exception in getEventState(): " + error.stack);
        return error;
    }
});

async function getQuestionFunction(teamCode, deviceId) {
    //Codes: EventNotStarted, EventEnded, EndGame, EventBlocked, UnlockedAllQuestions, getEventStatus(): EventRunning
    let masterSnapshotData, teamSnapshotData;
    let est, qri, eet; //est=eventStartTime,qri=questionReleaseInterval
    let teamName = null;
    let maxQuestionReleasePerStage = 0;
    let currentTime = new Date();
    let maxHintsAllowed, totalHintsUsed;
    let currentTeamScore;
    let availableTime;
    try {
        // TBD: Call init on first call, may be not, will check it later
        // Read all master data
        await getDatabase()
            .ref("/master")
            .get()
            .then((masterSnapshot) => {
                masterSnapshotData = masterSnapshot;
                est = new Date(
                    masterSnapshotData.child("eventStartTime").val()
                );
                eet = new Date(masterSnapshotData.child("eventEndTime").val());
                maxQuestionReleasePerStage = masterSnapshotData
                    .child("maxQuestionReleasePerStage")
                    .val();
                // Read questionReleaseInterval and convert minutes to milliseconds
                qri =
                    masterSnapshotData.child("questionReleaseInterval").val() *
                    60 *
                    1000;
            });
        logger.debug(
            "masterSnapshotData: " + JSON.stringify(masterSnapshotData)
        );

        // Read all team data
        await getDatabase()
            .ref(`/teams/${teamCode}`)
            .get()
            .then((teamSnapshot) => {
                teamSnapshotData = teamSnapshot;
                teamName = teamSnapshotData.child("teamName").val();
            });
        // logger.debug("teamSnapshotData: " + JSON.stringify(teamSnapshotData));
        currentTeamScore = getCurrentScore(
            masterSnapshotData,
            teamSnapshotData
        );

        let eventState = getEventStateUtil(masterSnapshotData);
        if (eventState != null) {
            eventState.teamScore = currentTeamScore;
            return eventState;
        }

        // logger.debug("Temp 7");
        // calculate eventCurrentLevel
        let totalIntervalFromEventStart = currentTime - est;
        let eventCurrentStage = Math.floor(totalIntervalFromEventStart / qri);

        //this will freeze last stage till event ends (we reached here because event is running)
        let totalStages = Math.floor(
            masterSnapshotData.child("questionDetails").numChildren() /
                maxQuestionReleasePerStage
        );
        eventCurrentStage = Math.min(eventCurrentStage, totalStages - 1);
        logger.debug(
            "Get Question: : eventCurrentStage" +
                eventCurrentStage +
                ", totalStages" +
                totalStages
        );
        //CurrentQuestionOfEvent
        let eventCurrentLevel =
            1 + eventCurrentStage * maxQuestionReleasePerStage;
        logger.debug(
            "totalIntervalFromEventStart(in Mints):" +
                totalIntervalFromEventStart / 60 / 1000 +
                ", eventCurrentStage:" +
                eventCurrentStage
        );
        // calculate available time via last question release time
        let qrt = new Date(est.valueOf() + Number(eventCurrentStage * qri));

        //if last stage available time should be according to event end time
        if (eventCurrentStage == totalStages - 1){
            availableTime = eet - currentTime;
        }
        else availableTime = qri - (currentTime - qrt);

        //assuming integer me hoge stages like 7days not 7.5days
        let auxTotalStages = Math.floor((eet - est) / qri);
        console.log("mm0", auxTotalStages);

        let extraStages = auxTotalStages - totalStages;

        let intervalsSnapshot = masterSnapshotData.child("blockIntervals");
        let noOfIntervals = intervalsSnapshot.numChildren();

        logger.info("mm1", availableTime / (1000 * 60 * 60));


        //In this part we are removing the blockage time (blockIntervals tiime) from the total available time

        //if extra time has no blockage then comment out if part
        if (eventCurrentStage == totalStages - 1) {
            let currentTimeTemp = currentTime;

            logger.info("mm3", totalStages, auxTotalStages);

            for (let i = totalStages; i <= auxTotalStages; i++) {
                let nextStageStarts = new Date(est.valueOf() + Number(i * qri));

                let currentTimeTempDay = currentTimeTemp;

                logger.info("madhav1", currentTimeTempDay, nextStageStarts);

                let stday = currentTimeTempDay.getDate();
                let enday = nextStageStarts.getDate();

                logger.info("mm2", stday, enday);

                for (; stday <= enday; stday++) {
                    currentTimeTempDay.setDate(stday);
                    logger.info("ss0: ", stday, enday, currentTimeTempDay);

                    for (let j = 0; j < noOfIntervals; j++) {
                        let startTime = getDateObjectFromTime(
                            intervalsSnapshot.child(j).child("start").val(),
                            currentTimeTempDay
                        );
                        let endTime = getDateObjectFromTime(
                            intervalsSnapshot.child(j).child("end").val(),
                            currentTimeTempDay
                        );
                        logger.info("ssss", startTime, endTime);
                        if (
                            startTime > currentTimeTempDay &&
                            startTime < nextStageStarts
                        ) {
                            logger.info(
                                "checker1",
                                availableTime / (1000 * 60 * 60)
                            );
                            logger.info("sss", startTime, endTime);
                            if (endTime < nextStageStarts)
                                availableTime -= endTime - startTime;
                            else availableTime -= nextStageStarts - startTime;
                            logger.info(
                                "checker2",
                                (endTime - startTime) / (1000 * 60 * 60),
                                availableTime / (1000 * 60 * 60)
                            );
                        }
                    }

                    currentTimeTempDay.setHours(0, 0, 0, 0);
                }

                currentTimeTemp = new Date(
                    currentTimeTemp.valueOf() +
                        Number(nextStageStarts - currentTimeTemp)
                );
            }
        } else {
            //assuming month or year nhi change hoga during event
            //important assumption: if we have to block from evening 5 to morning 5.. we should have two block intervals 1: evening 5 to midnight, 2: midnight to morning 5
            let nextStageStarts = new Date(
                est.valueOf() + Number((eventCurrentStage + 1) * qri)
            );
            let currentTimeTemp = currentTime;

            let stday = currentTimeTemp.getDate();
            let enday = nextStageStarts.getDate();

            for (; stday <= enday; stday++) {
                currentTimeTemp.setDate(stday);

                for (let i = 0; i < noOfIntervals; i++) {
                    let startTime = getDateObjectFromTime(
                        intervalsSnapshot.child(i).child("start").val(),
                        currentTimeTemp
                    );
                    let endTime = getDateObjectFromTime(
                        intervalsSnapshot.child(i).child("end").val(),
                        currentTimeTemp
                    );

                    if (
                        startTime > currentTimeTemp &&
                        startTime < nextStageStarts
                    ) {
                        if (endTime < nextStageStarts)
                            availableTime -= endTime - startTime;
                        else availableTime -= nextStageStarts - startTime;
                    }
                }

                currentTimeTemp.setHours(0, 0, 0, 0);
            }
        }

        logger.debug(
            "currentTime:" +
                currentTime +
                "qrt:" +
                qrt +
                ", availableTime:" +
                availableTime
        );

        // logger.debug("Temp 8");
        // Calculate current question level for current team
        let teamCurrentLevel = teamSnapshotData.child("currentLevel").val();
        let quesCurrentLevel = 0;
        if (eventCurrentLevel > teamCurrentLevel) {
            quesCurrentLevel = eventCurrentLevel;
            teamCurrentLevel = quesCurrentLevel;
            await getDatabase()
                .ref(`/teams/${teamCode}`)
                .update({ currentLevel: teamCurrentLevel });
        } else {
            quesCurrentLevel = teamCurrentLevel;
            if (
                teamCurrentLevel - eventCurrentLevel >=
                    maxQuestionReleasePerStage &&
                masterSnapshotData.child("questionDetails").numChildren() >=
                    quesCurrentLevel
            ) {
                let customCode = "UnlockedAllQuestions";
                let userMsg =
                    "Good Job !! \nWait for the next question to release.";
                logger.debug("UnlockedAllQuestions: Questions finished");
                return {
                    code: customCode,
                    msg: userMsg,
                    teamScore: currentTeamScore,
                    time: availableTime,
                };
            }
        }
        // logger.debug("Get Question: : eventCurrentLevel"+eventCurrentLevel+", teamCurrentLevel"+teamCurrentLevel+", quesCurrentLevel"+quesCurrentLevel);
        // logger.debug("Temp 9");
        // Check for End Game, if questions finished.
        if (
            masterSnapshotData.child("questionDetails").numChildren() <
            quesCurrentLevel
        ) {
            let customCode = "EndGame";
            let userMsg = "Game End !! \nAll the questions completed.";
            logger.debug("EndGame: Questions finished");
            return {
                code: customCode,
                msg: userMsg,
                teamScore: currentTeamScore,
                time: null,
            };
        }
        // logger.debug("Temp 10");
        // Get current team score
        let positiveTeamScore = 0,
            negativeScoreBalancer = 0,
            baseCumulativeScore = 0,
            teamScore = 0;
        positiveTeamScore = teamSnapshotData
            .child("score")
            .child("teamScore")
            .val();
        negativeScoreBalancer = teamSnapshotData
            .child("score")
            .child("negativeScoreBalancer")
            .val();
        baseCumulativeScore = masterSnapshotData
            .child("baseCumulativeScore")
            .child(quesCurrentLevel - 1 + "")
            .val();
        teamScore =
            baseCumulativeScore + positiveTeamScore + negativeScoreBalancer;
        // logger.debug("Temp 11");
        // Get Hint if unlocked
        let hint = null;
        if (
            teamSnapshotData.child("scoreCard").exists() &&
            teamSnapshotData
                .child("scoreCard")
                .child(quesCurrentLevel + "")
                .child("hintUsed")
                .val() == true
        ) {
            hint = masterSnapshotData
                .child("questionDetails")
                .child(quesCurrentLevel)
                .child("hint")
                .val();
        }

        totalHintsUsed = teamSnapshotData.child("totalHintsUsed").val();
        maxHintsAllowed = masterSnapshotData.child("maxHintsAllowed").val();

        let hintVisibility = masterSnapshotData.child("hintVisibility").val();

        // let intervalsSnapshot = masterSnapshotData.child("blockIntervals");
        // let noOfIntervals = intervalsSnapshot.numChildren();
        // let totalPauseInterval = 0;
        // logger.debug("Number of Block Intervals: " + noOfIntervals);
        // for (let i = 0; i < noOfIntervals; i++) {
        //     let startTime = getDateObjectFromTime(
        //         intervalsSnapshot.child(i).child("start").val(),
        //         currentTime
        //     );
        //     let endTime = getDateObjectFromTime(
        //         intervalsSnapshot.child(i).child("end").val(),
        //         currentTime
        //     );
        //     totalPauseInterval = totalPauseInterval + (endTime - startTime);
        // }

        // logger.debug("Temp 12");
        const strReturn = {
            teamName: teamName,
            // time: availableTime-totalPauseInterval,
            time: availableTime,
            level: quesCurrentLevel,
            rank: teamScore + "",
            maxRank: 50,
            question: masterSnapshotData
                .child("questionDetails")
                .child(quesCurrentLevel)
                .child("questionText")
                .val(),
            hint: hint,
            ansLength: masterSnapshotData
                .child("questionDetails")
                .child(quesCurrentLevel)
                .child("answer")
                .val().length,
            hintVisibility: hintVisibility == null ? false : hintVisibility,
            availableHints: maxHintsAllowed - totalHintsUsed,
            totalHints: maxHintsAllowed,
        };

        logger.debug(
            "Get Question Function Return Object: " + JSON.stringify(strReturn)
        );

        return strReturn;
    } catch (error) {
        logger.debug("Exception in getQuestion(): " + error.stack);
        return error;
    }
}

exports.getQuestion = onCall(async (req) => {
    let question = await getQuestionFunction(req.data.teamCode, req.data.deviceId);
    return question;
});

exports.checkPwdAndRegister = onCall(async (req) => {
    try {
        const teamCode = req.data.teamCode;
        const deviceId = req.data.deviceId;
        let wrongTeamCode = false;
        let registeredSuccessfully = false;
        let alreadyRegistered;
        let maxParticipantsInTeam = 0;
        let masterSnapshotData;
        let teamSnapshotData;
        let currentTeamScore;
        let registeredDevices = [];
        logger.info(
            "checkPwdAndRegister Call: teamCode: " +
                teamCode +
                ", deviceId: " +
                deviceId
        );

        await getDatabase()
            .ref("/master")
            .get()
            .then(async (masterSnapshot) => {
                masterSnapshotData = masterSnapshot;
            });
        // Read all team data
        await getDatabase()
            .ref(`/teams/${teamCode}`)
            .get()
            .then((teamSnapshot) => {
                teamSnapshotData = teamSnapshot;
                teamName = teamSnapshotData.child("teamName").val();
            });
        // logger.debug("teamSnapshotData: " + JSON.stringify(teamSnapshotData));
        currentTeamScore = getCurrentScore(
            masterSnapshotData,
            teamSnapshotData
        );

        let eventState = getEventStateUtil(masterSnapshotData);
        if (eventState != null) {
            eventState.teamScore = currentTeamScore;
            return eventState;
        }

        logger.info("here");

        await getDatabase()
            .ref(`/devices/${deviceId}`)
            .get()
            .then((snapshot) => {
                logger.debug("Inside device check.");
                if (snapshot.exists()) {
                    logger.debug(
                        "Inside snapshot exist." + JSON.stringify(snapshot)
                    );
                    alreadyRegistered = snapshot.val();
                    // logger.debug("Snapshot val." + alreadyRegistered);
                }
            });

        if (alreadyRegistered) {
            logger.debug("If device already registered: " + alreadyRegistered);
            return {
                alreadyRegistered: alreadyRegistered,
            };
        }

        // await getDatabase()
        //     .ref("/master/maxParticipantsInTeam")
        //     .get()
        //     .then(async (maxParticipantsInTeamSnapshot) => {
        //         maxParticipantsInTeam = maxParticipantsInTeamSnapshot.val();
        //     });

        maxParticipantsInTeam = masterSnapshotData
            .child("maxParticipantsInTeam")
            .val();

        await getDatabase()
            .ref(`/teams/${teamCode}`)
            .get()
            .then(async (teamSnapshot) => {
                if (!teamSnapshot.exists()) {
                    wrongTeamCode = true;
                } else {
                    teamSnapshot = teamSnapshot.val();
                    registeredDevices =
                        teamSnapshot.registeredDevices == null ||
                        teamSnapshot.registeredDevices == undefined
                            ? []
                            : teamSnapshot.registeredDevices;
                    if (registeredDevices.length < maxParticipantsInTeam) {
                        registeredDevices.push(deviceId);
                        await getDatabase()
                            .ref("/devices")
                            .update({
                                [deviceId]: teamCode,
                            });
                        await getDatabase().ref(`/teams/${teamCode}`).update({
                            registeredDevices: registeredDevices,
                        });
                        registeredSuccessfully = true;
                    }
                }
            });

        const returnObject = {
            wrongTeamCode: wrongTeamCode,
            registeredSuccessfully: registeredSuccessfully,
            registeredDevices: registeredDevices,
        };

        logger.info("Return Object: " + JSON.stringify(returnObject));

        return returnObject;
    } catch (error) {
        logger.debug("Error in checkPwdAndRegister(): " + error.stack);
        return {
            error: error,
        };
    }
});

exports.isRegisteredDevice = onCall(async (req) => {
    const deviceId = req.data.deviceId;
    let registrationStatus = false;
    let teamCode = null;
    logger.debug("isRegisteredDevice(): deviceId: " + deviceId);

    await getDatabase()
        .ref("/devices")
        .get()
        .then((snapshot) => {
            if (snapshot.exists()) {
                snapshot.forEach((deviceSnapshot) => {
                    if (deviceSnapshot.key === deviceId) {
                        registrationStatus = true;
                        teamCode = deviceSnapshot.val();
                        logger.info(
                            "isRegisteredDevice(): teamCode=" +
                                teamCode +
                                ", status=" +
                                registrationStatus
                        );
                        return;
                    }
                });
            }
        });
    return {
        registrationStatus: registrationStatus,
        teamPassword: teamCode,
    };
});

exports.addQuestion = onCall(async (req) => {
    try {
        const question = {
            answer: req.data.answer,
            earlyBird: {
                answered: false,
            },
            hint: req.data.hint,
            questionText: req.data.questionText,
        };

        await getDatabase().ref(`/master/questionDetails/${req.data.level}`).set(question);

        return { question };
    } catch (error) {
        logger.debug("Exception in addQuestion(): " + error.stack);
        return error;
    }
});
