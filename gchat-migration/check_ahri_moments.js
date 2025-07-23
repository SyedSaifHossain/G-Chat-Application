const admin = require('firebase-admin');
const serviceAccount = require('./serviceAccountKey.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  databaseURL: "https://g-chat-d1bcb-default-rtdb.firebaseio.com",
  projectId: "g-chat-d1bcb"
});

const firestore = admin.firestore();

async function checkAhriMoments() {
  console.log('检查Ahri账号的动态数据...\n');
  
  // Ahri的账号ID
  const ahriUserId = "uFAsQOjB0nTCWuHXCxPEiFfhKnj2";
  
  try {
    // 检查Ahri的用户信息
    console.log('=== Ahri用户信息 ===');
    const userDoc = await firestore.collection('users').doc(ahriUserId).get();
    
    if (userDoc.exists) {
      const userData = userDoc.data();
      console.log(`✅ Ahri用户存在: ${userData.name || userData.email || '未知'}`);
      console.log(`用户ID: ${ahriUserId}`);
      console.log(`用户数据: ${JSON.stringify(userData, null, 2)}`);
    } else {
      console.log('❌ Ahri用户在Firestore中不存在');
    }
    
    // 检查所有动态
    console.log('\n=== 所有动态数据 ===');
    const momentsSnapshot = await firestore.collection('moments').get();
    console.log(`总动态数: ${momentsSnapshot.size}`);
    
    let ahriMoments = 0;
    for (const doc of momentsSnapshot.docs) {
      const momentData = doc.data();
      console.log(`\n动态 ${doc.id}:`);
      console.log(`  userId: ${momentData.userId}`);
      console.log(`  text: ${momentData.text}`);
      console.log(`  timestamp: ${momentData.timestamp}`);
      console.log(`  day: ${momentData.day}, month: ${momentData.month}`);
      
      if (momentData.userId === ahriUserId) {
        ahriMoments++;
        console.log(`  ✅ 这是Ahri的动态`);
      } else {
        console.log(`  ❌ 不是Ahri的动态`);
      }
    }
    
    console.log(`\n=== 总结 ===`);
    console.log(`Ahri的动态数量: ${ahriMoments}`);
    
    if (ahriMoments === 0) {
      console.log('\n❌ Ahri没有发布过动态，或者动态数据有问题');
      console.log('可能的原因:');
      console.log('1. Ahri还没有发布过动态');
      console.log('2. 动态数据中的userId字段不正确');
      console.log('3. 动态数据没有正确保存到Firestore');
    } else {
      console.log('\n✅ Ahri有动态数据，应该能在应用中看到');
    }
    
  } catch (error) {
    console.error('检查失败:', error);
  }
}

checkAhriMoments(); 