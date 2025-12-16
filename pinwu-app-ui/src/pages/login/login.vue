<template>
  <div class="login-container">
    <div class="logo-area">
      <div class="logo-text">PINWU</div>
      <div class="slogan">手机号快捷登录</div>
    </div>

    <div class="form-area">
      <div class="input-group">
        <input class="input" v-model="form.mobile" type="number" maxlength="11" placeholder="请输入手机号" />
      </div>

      <div class="input-group code-group">
        <input class="input code-input" v-model="form.code" type="number" maxlength="6" placeholder="验证码" />
        
        <div class="code-btn" :class="{ disabled: countdown > 0 }" @click="handleSendCode">
          {{ countdown > 0 ? `${countdown}s后重新获取` : '获取验证码' }}
        </div>
      </div>

      <button class="btn-login" @click="handleLogin" :disabled="loading">
        {{ loading ? '登录中...' : '立 即 登 录' }}
      </button>
      
      <div class="tips">
        <p>测试说明：点击获取后，验证码会自动填入</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue';
import request from '../../utils/request.js';

const loading = ref(false);
const countdown = ref(0);
let timer = null;

const form = reactive({
  mobile: '', 
  code: ''
});

// --- 核心逻辑 1: 发送验证码 (Mock模式) ---
const handleSendCode = async () => {
  if (countdown.value > 0) return;
  if (!/^1[3-9]\d{9}$/.test(form.mobile)) {
    return uni.showToast({ title: '请输入正确的手机号', icon: 'none' });
  }

  try {
    // 调用后端 @PostMapping("/sendCode")
    // 注意：后端是 @RequestParam，所以参数最好拼在 URL 上，或者发 form-data
    // 这里为了稳妥，直接拼在 URL 上，确保后端能收到
    const res = await request({
      url: `/app/auth/sendCode?mobile=${form.mobile}`,
      method: 'POST'
    });
    
    // ★★★ 关键点：直接把后端返回的验证码填入输入框 ★★★
    // 假设后端返回结构是: { code: 200, msg: "发送成功", data: "1234" }
    form.code = res.data; 
    
    uni.showToast({ title: '验证码已自动填入', icon: 'success' });

    // 开始倒计时 UI
    countdown.value = 60;
    timer = setInterval(() => {
      countdown.value--;
      if (countdown.value <= 0) clearInterval(timer);
    }, 1000);

  } catch (e) {
    console.error(e);
  }
};

// --- 核心逻辑 2: 登录 ---
const handleLogin = async () => {
  if (!form.mobile || !form.code) {
    return uni.showToast({ title: '请填写完整', icon: 'none' });
  }

  loading.value = true;
  try {
    // 调用后端 @PostMapping("/login")
    // 后端接收 @RequestBody AppLoginBody
    const res = await request({
      url: '/app/auth/login',
      method: 'POST',
      data: {
        mobile: form.mobile,
        code: form.code, // 或者是 smsCode，取决于你 AppLoginBody 的字段名，这里暂定 code
        loginType: 'sms'
        // uuid: ... 如果你有图形验证码逻辑，这里要传 uuid，没有则忽略
      }
    });

    console.log('登录返回：', res);  // ⭐ 看看返回结构
    console.log('Token：', res.token);  // ⭐ 确认 token 字段名
  

    // 保存 Token
    uni.setStorageSync('token', res.token); // 注意：这里取 res.token，取决于你后端 success().put("token", token) 的结构，如果是 res.data.token 请自行调整
    
    uni.showToast({ title: '登录成功' });
    
    // 跳转首页
    setTimeout(() => {
      uni.switchTab({ url: '/pages/index/index' });
    }, 1000);

  } catch (e) {
    console.error(e);
  } finally {
    loading.value = false;
  }
};
</script>

<style lang="scss">
/* 保持之前的配色，只调整布局 */
.login-container {
  min-height: 100vh; background: #fff; padding: 0 30px; display: flex; flex-direction: column;
}
.logo-area {
  margin-top: 80px; margin-bottom: 50px; text-align: center;
  .logo-text { font-size: 36px; font-weight: 900; color: $pin-primary; letter-spacing: 2px; }
  .slogan { font-size: 14px; color: $pin-text-placeholder; margin-top: 10px; }
}
.input-group {
  margin-bottom: 20px;
  .input {
    width: 100%; height: 50px; background: $pin-bg-color; border-radius: 25px; padding: 0 20px; font-size: 16px; color: $pin-text-main; box-sizing: border-box;
  }
}
.code-group {
  display: flex; justify-content: space-between; align-items: center;
  .code-input { flex: 1; margin-right: 10px; }
  .code-btn {
    width: 110px; height: 50px; line-height: 50px; text-align: center; background: #fff; border: 1px solid $pin-primary; color: $pin-primary; border-radius: 25px; font-size: 14px;
    &.disabled { border-color: #ccc; color: #ccc; }
    &:active:not(.disabled) { background: rgba(39,186,155,0.1); }
  }
}
.btn-login {
  width: 100%; height: 50px; line-height: 50px; background: linear-gradient(to right, $pin-primary, $pin-success); color: #fff; border-radius: 25px; font-size: 16px; font-weight: bold; margin-top: 30px; border: none;
  &:active { opacity: 0.9; }
}
.tips { margin-top: 30px; text-align: center; color: #999; font-size: 12px; }
</style>