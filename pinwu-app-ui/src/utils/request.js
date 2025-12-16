// src/utils/request.js

// ★★★ 重点：如果你是用手机测，这里不能写 localhost，要写你电脑的局域网IP (如 192.168.1.5:8081)
// ★★★ 如果只用电脑浏览器测，写 localhost:8081 没问题
const BASE_URL = 'http://localhost:8081'; 

const request = (options) => {
  return new Promise((resolve, reject) => {
    // 1. 取 Token
    const token = uni.getStorageSync('token');

    // 2. 组装 Header
    let header = {
      'Content-Type': 'application/json',
      ...options.header
    };
    if (token) {
      header['Authorization'] = 'Bearer ' + token;
    }

    // 3. 发请求
    uni.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      header: header,
      success: (res) => {
        const code = res.data.code;
        // 成功
        if (code === 200) {
          resolve(res.data);
        } 
        // 未登录
        else if (code === 401) {
          uni.showToast({ title: '请先登录', icon: 'none' });
          setTimeout(() => {
            uni.reLaunch({ url: '/pages/login/login' });
          }, 1000);
          reject(res.data);
        } 
        // 其他业务错误
        else {
          uni.showToast({ title: res.data.msg || '请求失败', icon: 'none' });
          reject(res.data);
        }
      },
      fail: (err) => {
        uni.showToast({ title: '服务器连接失败', icon: 'none' });
        console.error('API Error:', err);
        reject(err);
      }
    });
  });
};

export default request;