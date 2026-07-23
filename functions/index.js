const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

const db = admin.firestore();

exports.onNoiseFlagCreated = functions.firestore
    .document('flags/{flagId}')
    .onCreate(async (snap, context) => {
        const flag = snap.data();
        const buildingId = flag.buildingId || 'default_building';
        const timeWindow = flag.timeWindow;

        // 1. Fetch Building Config
        const configSnap = await db.collection('buildings').document(buildingId).get();
        const config = configSnap.exists ? configSnap.data() : { consensusThreshold: 2 };

        // 2. Fetch all flags in this time window and building
        const flagsSnap = await db.collection('flags')
            .where('buildingId', '==', buildingId)
            .where('timeWindow', '==', timeWindow)
            .get();

        const activeFlags = flagsSnap.docs.map(d => ({ id: d.id, ...d.data() }));

        if (activeFlags.length < config.consensusThreshold) {
            console.log(`Threshold not met: ${activeFlags.length}/${config.consensusThreshold}`);
            return null;
        }

        // 3. Consensus Logic: Find non-adjacent flaggers and identify common neighbor
        // Fetch adjacency map for the building
        const adjacencySnap = await db.collection('adjacencyMap').where('buildingId', '==', buildingId).get();
        const adjacencyMap = {};
        adjacencySnap.forEach(doc => {
            adjacencyMap[doc.id] = doc.data().neighborRoomIds || [];
        });

        // Simple algorithm:
        // For each pair of flaggers, if they are NOT adjacent, find their common neighbors.
        // The common neighbor with the most "votes" is the target.

        const candidates = {};

        for (let i = 0; i < activeFlags.length; i++) {
            for (let j = i + 1; j < activeFlags.length; j++) {
                const f1 = activeFlags[i];
                const f2 = activeFlags[j];

                // Check non-adjacency rule
                const neighborsOf1 = adjacencyMap[f1.flaggerRoomId] || [];
                if (neighborsOf1.includes(f2.flaggerRoomId)) continue; // They are neighbors, skip

                // Find common neighbors
                const neighborsOf2 = adjacencyMap[f2.flaggerRoomId] || [];
                const common = neighborsOf1.filter(n => neighborsOf2.includes(n));

                common.forEach(roomId => {
                    candidates[roomId] = (candidates[roomId] || 0) + 1;
                });
            }
        }

        // Find the winner
        let targetRoomId = null;
        let maxVotes = 0;
        for (const [roomId, votes] of Object.entries(candidates)) {
            if (votes > maxVotes) {
                maxVotes = votes;
                targetRoomId = roomId;
            }
        }

        if (targetRoomId) {
            console.log(`Confirmed Incident! Target: ${targetRoomId}, Votes: ${maxVotes}`);

            // 4. Create Confirmed Incident
            await db.collection('confirmedIncidents').add({
                roomId: targetRoomId,
                buildingId: buildingId,
                timestamp: admin.firestore.FieldValue.serverTimestamp(),
                severity: 1, // Basic severity
                sources: activeFlags.map(f => f.flaggerRoomId)
            });

            // 5. Send FCM Nudge
            // Find user in that room
            const userSnap = await db.collection('users').where('roomId', '==', targetRoomId).get();
            const registrationTokens = [];
            userSnap.forEach(doc => {
                if (doc.data().fcmToken) registrationTokens.push(doc.data().fcmToken);
            });

            if (registrationTokens.length > 0) {
                const message = {
                    notification: {
                        title: 'EchoEase Nudge',
                        body: 'Your neighbors have noticed elevated noise levels. Please be mindful of your surroundings. — Building Management'
                    },
                    tokens: registrationTokens,
                };
                await admin.messaging().sendMulticast(message);
            }
        }

        return null;
    });
