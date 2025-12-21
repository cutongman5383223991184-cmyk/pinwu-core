<template>
  <div class="publish-page">
    <div class="upload-wrap">
      <div class="img-item" v-for="(img, index) in fileList" :key="index">
        <image class="pic" :src="img" mode="aspectFill"></image>
        <div class="close" @click="removeImg(index)">×</div>
        <div class="main-tag" v-if="index === 0">主图</div>
      </div>
      <div class="upload-box" @click="handleUpload" v-if="fileList.length < 9">
        <div class="plus">+</div>
        <div class="text">添加图片</div>
      </div>
      <div class="ai-btn-float" v-if="fileList.length > 0 && !aiLoading" @click="handleAiAnalyze">
        <text>✨ AI 识别填充</text>
      </div>
    </div>

    <div class="form-card">
      <div class="input-row">
        <input class="title-input" v-model="form.title" placeholder="标题 品牌型号 (必填)" @input="onTitleInput" />
        <div class="ai-text-btn" @click="handleAiGenerate">AI 帮写</div>
      </div>
      <textarea class="desc-input" v-model="form.detail" placeholder="描述一下宝贝的细节、新旧程度..."></textarea>
      <div class="row">
        <div class="label">标签</div>
        <input class="input" v-model="tagInput" placeholder="空格分隔，如: 教材 英语" />
      </div>
    </div>

    <div class="form-card">
      <div class="mode-switch">
        <div class="mode-item" :class="{active: locationMode === 'map'}" @click="locationMode = 'map'">地图选点</div>
        <div class="mode-item" :class="{active: locationMode === 'manual'}" @click="locationMode = 'manual'">手动输入</div>
      </div>

      <div v-if="locationMode === 'map'">
        <div class="row" @click="chooseLocation">
          <div class="label">📍 交易地点</div>
          <div class="value-box">
             <input class="addr-input" v-model="form.locationName" placeholder="点击打开地图选择" disabled />
             <div class="area-text" v-if="form.province">
               {{ form.province }} · {{ form.city }} · {{ form.region }}
             </div>
          </div>
          <div class="arrow">></div>
        </div>
      </div>

      <div v-else>
        <div class="row">
          <div class="label">所在省份</div>
          <input class="input" v-model="manualForm.province" placeholder="如: 江苏省" />
        </div>
        <div class="row">
          <div class="label">所在城市</div>
          <input class="input" v-model="manualForm.city" placeholder="如: 南京市" />
        </div>
        <div class="row">
          <div class="label">区/县</div>
          <input class="input" v-model="manualForm.region" placeholder="如: 浦口区" />
        </div>
        <div class="row">
          <div class="label">详细地址</div>
          <input class="input" v-model="manualForm.detail" placeholder="街道、小区、楼号" />
        </div>
        <div class="tip-text">发布时将自动计算距离</div>
      </div>
    </div>

    <div class="form-card">
      <div class="row">
        <div class="label">展示价格</div>
        <input class="input price" type="digit" v-model="form.price" placeholder="¥ 0.00" @input="onPriceInput" />
      </div>
      <div class="row">
        <div class="label">入手原价</div>
        <input class="input" type="digit" v-model="form.originalPrice" placeholder="¥ 0.00" />
      </div>
    </div>

    <div class="sku-card">
      <div class="card-header">
        <div class="title">规格设置</div>
        <div class="add-btn" @click="addSku">+ 添加规格</div>
      </div>
      
      <div class="sku-item" v-for="(sku, index) in form.skuList" :key="index">
        <div class="sku-main-row">
          <div class="sku-img-box" @click="handleSkuImgUpload(index)">
            <image v-if="sku.skuPic" :src="sku.skuPic" class="s-img" mode="aspectFill"></image>
            <div v-else class="s-plus">+</div>
          </div>
          
          <div class="sku-info">
            <div class="sku-name-row">
              <input class="sku-name" v-model="sku.skuName" placeholder="规格名 (如: 高数)" @focus="onSkuNameFocus(index)"/>
              <div class="sku-del" @click="removeSku(index)" v-if="form.skuList.length > 1">删除</div>
            </div>
            <div class="sku-val-row">
               <div class="mini-field">
                 <text>¥</text>
                 <input type="digit" v-model="sku.price" placeholder="0.00" />
               </div>
               <div class="mini-field stock">
                 <text>库</text>
                 <input type="number" v-model="sku.stock" placeholder="1" />
               </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="form-card">
      <div class="row">
        <div class="label">联系方式</div>
        <picker :range="['微信号', '手机号', 'QQ号']" @change="onContactTypeChange">
          <div class="picker-val">{{ contactTypeText }} ▼</div>
        </picker>
        <input class="input" v-model="form.contactValue" placeholder="请输入号码" style="text-align: right;" />
      </div>
    </div>

    <div class="footer">
      <button class="pub-btn" @click="submit" :disabled="loading || aiLoading">
        {{ aiLoading ? 'AI 思考中...' : (loading ? '发布中...' : '确认发布') }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, computed } from 'vue';
import request from '../../utils/request.js';

// --- 状态变量 ---
const loading = ref(false);
const aiLoading = ref(false);
const fileList = ref([]); 
const tagInput = ref(''); 
const isSkuEdited = ref(false);
const locationMode = ref('map'); // map | manual

// 手动输入地址表单
const manualForm = reactive({
  province: '',
  city: '',
  region: '',
  detail: ''
});

const contactTypes = ['微信号', '手机号', 'QQ号'];
const contactIndex = ref(0);
const contactTypeText = computed(() => contactTypes[contactIndex.value]);

// 主表单
const form = reactive({
  title: '',
  detail: '',
  price: '',
  originalPrice: '',
  locationName: '',
  province: '', 
  city: '',
  region: '',
  latitude: 0,
  longitude: 0,
  contactType: 1,
  contactValue: '',
  skuList: [{ skuName: '', skuPic: '', price: '', stock: 1 }]
});

// --- SKU 图片上传 ---
const handleSkuImgUpload = (index) => {
  uni.chooseImage({
    count: 1,
    success: (res) => {
      const filePath = res.tempFilePaths[0];
      uni.showLoading({ title: '上传中' });
      const token = uni.getStorageSync('token');
      uni.uploadFile({
        url: 'http://localhost:8081/app/common/upload',
        filePath: filePath, name: 'file',
        header: { 'Authorization': 'Bearer ' + token },
        success: (uploadRes) => {
          const data = JSON.parse(uploadRes.data);
          if (data.code === 200) {
            // 更新对应 SKU 的图片
            form.skuList[index].skuPic = data.url;
          }
        },
        complete: () => uni.hideLoading()
      });
    }
  });
};

// --- 地图模式 ---
const chooseLocation = () => {
  uni.chooseLocation({
    success: (res) => {
      form.locationName = res.name || res.address; 
      form.latitude = res.latitude;
      form.longitude = res.longitude;
      parseLocation(res.latitude, res.longitude);
    }
  });
};

const parseLocation = async (lat, lng) => {
  try {
    const res = await request({
      url: `/app/location/regeo?lat=${lat}&lng=${lng}`,
      method: 'GET'
    });
    if (res.code === 200 && res.data && res.data.province) {
      const data = res.data;
      form.province = data.province;
      form.city = data.city;
      form.region = data.region;
    }
  } catch (e) {}
};

// --- SKU 逻辑 ---
const addSku = () => {
  // 新增SKU时，默认使用主图作为SKU图 (如果主图存在)
  const defaultPic = fileList.value.length > 0 ? fileList.value[0] : '';
  form.skuList.push({ skuName: '', skuPic: defaultPic, price: form.price, stock: 1 });
  isSkuEdited.value = true;
};
const removeSku = (index) => {
  form.skuList.splice(index, 1);
  if (form.skuList.length === 0) {
    addSku();
    form.skuList[0].skuName = form.title;
    isSkuEdited.value = false;
  }
};
// 自动同步
const onTitleInput = (e) => {
  if (form.skuList.length === 1 && !isSkuEdited.value) form.skuList[0].skuName = e.detail.value;
};
const onPriceInput = (e) => {
  if (form.skuList.length === 1) form.skuList[0].price = e.detail.value;
};
const onSkuNameFocus = (index) => {
  if (index === 0) isSkuEdited.value = true;
};

// --- 主图上传 ---
const handleUpload = () => {
  uni.chooseImage({
    count: 1,
    success: (res) => {
      const filePath = res.tempFilePaths[0];
      uni.showLoading({ title: '上传中' });
      const token = uni.getStorageSync('token');
      uni.uploadFile({
        url: 'http://localhost:8081/app/common/upload',
        filePath: filePath, name: 'file',
        header: { 'Authorization': 'Bearer ' + token },
        success: (uploadRes) => {
          const data = JSON.parse(uploadRes.data);
          if (data.code === 200) {
            fileList.value.push(data.url);
            // 如果是第一张图（主图），且SKU还没有图，给第一个SKU也赋上
            if (fileList.value.length === 1 && form.skuList.length === 1 && !form.skuList[0].skuPic) {
              form.skuList[0].skuPic = data.url;
            }
          }
        },
        complete: () => uni.hideLoading()
      });
    }
  });
};
const removeImg = (i) => fileList.value.splice(i, 1);

// ... AI 逻辑省略(保持不变) ... 
const handleAiAnalyze =async () => {
  // 1. 校验：必须先上传图片
  if (fileList.value.length === 0) {
    return uni.showToast({ title: '请先上传一张主图', icon: 'none' });
  }

  // 2. 开启 Loading 状态
  aiLoading.value = true;
  uni.showLoading({ title: 'AI 正在识别...' });

  try {
    // 3. 调用后端接口
    const res = await request({
      url: '/app/ai/analyze-image',
      method: 'POST',
      data: {
        // 取第一张图作为主图进行识别
        imageUrl: fileList.value[0]
      }
    });

    // 4. 填充表单
    if (res.code === 200 && res.data) {
      const aiData = res.data;
      
      // 填充标题
      form.title = aiData.title || '';
      
      // 填充描述
      form.detail = aiData.description || '';
      
      // 填充标签 (后端返回的是数组，前端输入框是空格分隔的字符串)
      // 小技巧：把后端返回的 category 也加到标签里，增加曝光度
      let finalTags = aiData.tags || [];
      if (aiData.category) {
        finalTags.unshift(aiData.category); // 把分类插到第一个标签
      }
      // 去重并转为字符串
      tagInput.value = [...new Set(finalTags)].join(' ');

      // 如果只有一个 SKU 且未修改过，同步更新 SKU 名称
      if (form.skuList.length === 1 && !isSkuEdited.value) {
        form.skuList[0].skuName = form.title;
      }

      uni.showToast({ title: '识别成功，已自动填充', icon: 'success' });
    }
  } catch (e) {
    uni.showToast({ title: 'AI 识别失败，请重试', icon: 'none' });
    console.error(e);
  } finally {
    // 5. 关闭 Loading
    aiLoading.value = false;
    uni.hideLoading();
  }
}; 
const handleAiGenerate = () => {
  // 1. 使用 uni.showModal 显示输入框 (editable: true)
  uni.showModal({
    title: '✨ AI 帮写',
    editable: true, // 开启输入框
    placeholderText: '请输入关键词 (如: 99新 iPhone15 黑色)',
    success: async (modalRes) => {
      // 用户点击了确定，且输入了内容
      if (modalRes.confirm && modalRes.content) {
        
        const keyword = modalRes.content;
        aiLoading.value = true;
        uni.showLoading({ title: 'AI 正在创作...' });

        try {
          // 2. 调用后端接口
          const res = await request({
            url: '/app/ai/generate-text',
            method: 'POST',
            data: {
              keywords: keyword
            }
          });

          // 3. 填充表单 (逻辑同上)
          if (res.code === 200 && res.data) {
            const aiData = res.data;

            form.title = aiData.title || '';
            form.detail = aiData.description || '';
            
            let finalTags = aiData.tags || [];
            if (aiData.category) finalTags.unshift(aiData.category);
            tagInput.value = [...new Set(finalTags)].join(' ');

            if (form.skuList.length === 1 && !isSkuEdited.value) {
               form.skuList[0].skuName = form.title;
            }

            uni.showToast({ title: '生成完毕', icon: 'success' });
          }
        } catch (e) {
          uni.showToast({ title: '生成失败，请稍后重试', icon: 'none' });
        } finally {
          aiLoading.value = false;
          uni.hideLoading();
        }
      }
    }
  });
}; 

const onContactTypeChange = (e) => {
  contactIndex.value = e.detail.value;
  form.contactType = contactIndex.value + 1;
};

// --- 重置表单函数 ---
const resetForm = () => {
  // 1. 清空图片
  fileList.value = [];
  
  // 2. 清空普通变量
  tagInput.value = '';
  isSkuEdited.value = false;
  locationMode.value = 'map';
  
  // 3. 清空手动地址表单
  manualForm.province = '';
  manualForm.city = '';
  manualForm.region = '';
  manualForm.detail = '';

  // 4. 重置主表单核心数据
  // 注意：不能直接 form = {}，否则会丢失响应性，要用 Object.assign 或逐个赋值
  Object.assign(form, {
    title: '',
    detail: '',
    price: '',
    originalPrice: '',
    locationName: '',
    province: '',
    city: '',
    region: '',
    latitude: 0,
    longitude: 0,
    contactValue: '', // 如果想保留上次的联系方式，这行可以注释掉
    // 恢复默认的一个空 SKU
    skuList: [{ skuName: '', skuPic: '', price: '', stock: 1 }]
  });
};

// --- 提交核心逻辑 ---
const submit = async () => {
  if (!form.title || !form.price || fileList.value.length === 0) {
    return uni.showToast({ title: '请完善标题、价格和主图', icon: 'none' });
  }

  loading.value = true;

  try {
    // 1. 处理位置信息
    if (locationMode.value === 'manual') {
      // 校验手动输入
      if (!manualForm.province || !manualForm.city || !manualForm.detail) {
        throw new Error('请填写完整的省市区和详细地址');
      }
      
      const fullAddress = manualForm.province + manualForm.city + manualForm.region + manualForm.detail;
      uni.showLoading({ title: '计算位置中...' });
      
      // 调用后端地理编码
      const geoRes = await request({
        url: `/app/location/geo?address=${encodeURIComponent(fullAddress)}`,
        method: 'GET'
      });
      
      if (geoRes.code !== 200) throw new Error(geoRes.msg || '地址无法识别，请检查拼写');
      
      // 填充表单
      const geoData = geoRes.data;
      form.province = geoData.province;
      form.city = geoData.city;
      form.region = geoData.district;
      form.locationName = manualForm.detail; // 小区/楼号
      form.latitude = geoData.latitude;
      form.longitude = geoData.longitude;
      
    } else {
      // 地图模式校验
      if (!form.latitude || !form.longitude) throw new Error('请在地图上选择交易地点');
    }

    // 2. 构造提交参数
    const postData = {
      ...form,
      pic: fileList.value[0],
      tags: tagInput.value.trim() ? tagInput.value.split(' ') : [],
      price: parseFloat(form.price),
      originalPrice: parseFloat(form.originalPrice || 0),
      // 处理 SKU (带图片)
      skuList: form.skuList.map(s => ({
        skuName: s.skuName || form.title,
        price: parseFloat(s.price),
        stock: parseInt(s.stock),
        // 如果sku有图用自己的，没有图用主图
        skuPic: s.skuPic || fileList.value[0]
      }))
    };

    await request({ url: '/app/product/publish', method: 'POST', data: postData });
    uni.showToast({ title: '发布成功！', icon: 'success' });
    resetForm();
    setTimeout(() => { uni.switchTab({ url: '/pages/index/index' }); }, 1500);

  } catch (e) {
    uni.showToast({ title: e.message || '发布失败', icon: 'none' });
  } finally {
    loading.value = false;
    uni.hideLoading();
  }
};
</script>

<style lang="scss">
/* 解决底部遮挡问题 */
.publish-page { padding: 15px; padding-bottom: 130px; padding-bottom: calc(130px + constant(safe-area-inset-bottom)); padding-bottom: calc(130px + env(safe-area-inset-bottom)); background: $pin-bg-color; min-height: 100vh; }

/* 模式切换 */
.mode-switch {
  display: flex; background: #f0f0f0; border-radius: 8px; padding: 4px; margin-bottom: 15px;
  .mode-item {
    flex: 1; text-align: center; font-size: 14px; padding: 8px 0; border-radius: 6px; color: #666;
    &.active { background: #fff; color: $pin-primary; font-weight: bold; box-shadow: 0 2px 5px rgba(0,0,0,0.05); }
  }
}
.tip-text { font-size: 12px; color: #999; text-align: center; margin-top: 10px; }

/* SKU 图片样式 */
.sku-img-box {
  width: 50px; height: 50px; background: #fff; border-radius: 4px; margin-right: 10px; display: flex; align-items: center; justify-content: center; border: 1px dashed #ddd; overflow: hidden;
  .s-img { width: 100%; height: 100%; }
  .s-plus { font-size: 20px; color: #ccc; }
}

.sku-item {
  background: #f9f9f9; padding: 10px; border-radius: 8px; margin-bottom: 10px;
  .sku-main-row { display: flex; }
  .sku-info { flex: 1; display: flex; flex-direction: column; justify-content: space-between; }
  
  .sku-name-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 5px; }
  .sku-name { font-weight: bold; font-size: 14px; border-bottom: 1px dashed #ccc; width: 80%; }
  .sku-del { color: red; font-size: 12px; }
  
  .sku-val-row { display: flex; align-items: center; }
  .mini-field { 
    display: flex; align-items: center; background: #fff; border-radius: 4px; padding: 2px 5px; margin-right: 10px; border: 1px solid #eee;
    text { font-size: 10px; color: #999; margin-right: 3px; }
    input { width: 40px; font-size: 12px; text-align: center; }
    &.stock input { width: 30px; }
  }
}

/* 复用其他样式 (upload-wrap, form-card, footer等) 保持不变 */
/* ... 请确保之前的 CSS 还在 ... */
.ai-btn-float { position: absolute; bottom: 10px; right: 10px; background: linear-gradient(135deg, #6a11cb, #2575fc); color: #fff; font-size: 12px; padding: 5px 10px; border-radius: 20px; box-shadow: 0 2px 8px rgba(37,117,252,0.3); }
.upload-wrap { display: flex; flex-wrap: wrap; margin-bottom: 15px; position: relative; .img-item { width: 90px; height: 90px; margin-right: 10px; margin-bottom: 10px; position: relative; .pic { width: 100%; height: 100%; border-radius: 8px; } .close { position: absolute; top: -5px; right: -5px; background: red; color: white; border-radius: 50%; width: 18px; height: 18px; text-align: center; line-height: 16px; font-size: 12px; } .main-tag { position: absolute; bottom: 0; left: 0; width: 100%; background: rgba(0,0,0,0.5); color: #fff; font-size: 10px; text-align: center; border-radius: 0 0 8px 8px;} } .upload-box { width: 90px; height: 90px; background: #fff; border: 1px dashed #ccc; border-radius: 8px; display: flex; flex-direction: column; align-items: center; justify-content: center; color: #999; .plus { font-size: 30px; line-height: 30px; } .text { font-size: 12px; } } }
.form-card { background: #fff; border-radius: 10px; padding: 0 15px; margin-bottom: 15px; .input-row { display: flex; align-items: center; border-bottom: 1px solid #f5f5f5; } .title-input { height: 50px; font-size: 16px; font-weight: bold; width: 100%; flex: 1;} .ai-text-btn { font-size: 12px; color: #fff; background: $pin-warning; padding: 2px 8px; border-radius: 4px; margin-left: 10px; white-space: nowrap;} .desc-input { width: 100%; height: 100px; font-size: 14px; padding: 10px 0; border-bottom: 1px solid #f5f5f5; } .row { display: flex; align-items: center; height: 50px; border-bottom: 1px solid #f5f5f5; &:last-child { border-bottom: none; } .label { width: 80px; font-size: 15px; color: #333; } .input { flex: 1; font-size: 15px; text-align: right; } .price { color: $pin-danger; font-weight: bold; } .picker-val { font-size: 14px; color: #666; } .arrow { color: #ccc; margin-left: 5px; } } }
.value-box { flex: 1; text-align: right; display: flex; flex-direction: column; justify-content: center; .addr-input { font-size: 15px; color: #333; text-align: right; width: 100%; pointer-events: none; /* 禁止直接输入，必须点选 */ } .area-text { font-size: 10px; color: #999; margin-top: 2px; } .warning { color: #ff9900; } }
.sku-card { background: #fff; border-radius: 10px; padding: 15px; margin-bottom: 15px; .card-header { display: flex; justify-content: space-between; margin-bottom: 10px; align-items: center;} .title { font-weight: bold; font-size: 15px; } .sub-title { font-weight: normal; font-size: 12px; color: #999; margin-left: 5px; } .add-btn { color: $pin-primary; font-size: 14px; border: 1px solid $pin-primary; padding: 2px 8px; border-radius: 20px; } }
.footer { position: fixed; left: 0; width: 100%; background: #fff; padding: 10px 20px; box-shadow: 0 -2px 10px rgba(0,0,0,0.05); z-index: 100; box-sizing: border-box;bottom: calc(55px + env(safe-area-inset-bottom)); .pub-btn { background: linear-gradient(135deg, $pin-primary, $pin-success); color: #fff; border-radius: 25px; height: 44px; line-height: 44px; font-weight: bold; font-size: 16px; &::after { border: none; } } }
</style>