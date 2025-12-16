<template>
  <div class="my-page">
    <div class="header">
      <div class="user-box" v-if="isLogin">
        <image class="avatar" :src="user.avatar || '/static/logo.png'" mode="aspectFill"></image>
        <div class="info">
          <div class="nick">{{ user.nickname || user.phonenumber || '无名大侠' }}</div>
          <div class="desc">{{ user.phonenumber ? desensitize(user.phonenumber) : '' }} · 信用极好</div>
        </div>
      </div>
      <div class="user-box" v-else @click="goLogin">
        <div class="avatar">未</div>
        <div class="info">
          <div class="nick">点击登录</div>
          <div class="desc">登录后体验更多功能</div>
        </div>
      </div>
    </div>

    <div class="stats-card">
      <div class="stat-item" @click="showToast">
        <div class="num">0</div>
        <div class="label">我卖出的</div>
      </div>
      <div class="stat-item" @click="showToast">
        <div class="num">0</div>
        <div class="label">我买到的</div>
      </div>
    </div>
    
    <div class="menu-card">
      <div class="menu-item" @click="handleLogout">
        <text>退出登录</text>
        <text class="arrow">></text>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import request from '../../utils/request.js';

const isLogin = ref(false);
const user = ref({});

// 手机号脱敏工具
const desensitize = (phone) => {
  if (!phone) return '';
  return phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2');
}

const showToast = () => {
  uni.showToast({ title: '开发中...', icon: 'none' });
}

// 每次页面显示时触发
onShow(() => {
  const token = uni.getStorageSync('token');
  if (token) {
    isLogin.value = true;
    getUserInfo();
  } else {
    isLogin.value = false;
    user.value = {};
  }
});

const getUserInfo = async () => {
  try {
    // ★★★ 核心修改：调用 APP 专用接口 ★★★
    const res = await request({ 
      url: '/app/auth/getProfile',
      method: 'GET'
    });
    // 后端返回的是 AjaxResult.success(user对象)，也就是 res.data
    user.value = res.data; 
  } catch (e) {
    console.error("获取用户信息失败", e);
    // 如果 token 失效，重置状态
    isLogin.value = false;
    uni.removeStorageSync('token');
  }
};

const goLogin = () => {
  uni.reLaunch({ url: '/pages/login/login' });
};

const handleLogout = () => {
  uni.showModal({
    title: '提示',
    content: '确定要退出登录吗？',
    success: function (res) {
      if (res.confirm) {
        uni.removeStorageSync('token');
        isLogin.value = false;
        user.value = {};
        uni.reLaunch({ url: '/pages/login/login' });
      }
    }
  });
};
</script>

<style lang="scss">
.my-page { min-height: 100vh; background: $pin-bg-color; }
.header {
  height: 200px; background: $pin-primary; display: flex; align-items: center; padding: 0 20px;
  .user-box { display: flex; align-items: center; color: #fff; }
  .avatar { width: 60px; height: 60px; background: #fff; border-radius: 50%; margin-right: 15px; border: 2px solid rgba(255,255,255,0.3);}
  .nick { font-size: 20px; font-weight: bold; margin-bottom: 5px; }
  .desc { font-size: 14px; opacity: 0.8; }
}
.stats-card {
  margin: -50px 20px 15px; background: #fff; border-radius: 10px; padding: 20px 0; display: flex; justify-content: space-around; box-shadow: 0 5px 15px rgba(0,0,0,0.05);
  .stat-item { text-align: center; width: 45%; }
  .num { font-size: 18px; font-weight: bold; color: $pin-text-main; }
  .label { font-size: 12px; color: $pin-text-regular; margin-top: 5px; }
}
.menu-card {
  margin: 0 20px; background: #fff; border-radius: 10px; padding: 0 20px;
  .menu-item {
    height: 55px; display: flex; justify-content: space-between; align-items: center; color: $pin-text-main; font-size: 15px;
    .arrow { color: #ccc; }
  }
}
</style>