<template>
  <div class="confirm-page">
    <div class="address-card">
      <div class="addr-top">
        <text class="tag">默认</text>
        <text class="city">江苏省 南京市</text>
      </div>
      <div class="addr-detail">浦口区 宁六路219号 南京信息工程大学</div>
      <div class="user-info">范同学 138****0000</div>
      <div class="arrow">></div>
    </div>

    <div class="product-card" v-if="skuInfo.id">
      <div class="shop-name">卖家：{{ sellerName }}</div>
      <div class="goods-box">
        <image class="img" :src="productPic" mode="aspectFill"></image>
        <div class="info">
          <div class="title">{{ productTitle }}</div>
          <div class="sku-name">规格：{{ skuInfo.skuName }}</div>
          <div class="price-row">
            <text class="price">¥{{ skuInfo.price }}</text>
            <text class="count">x{{ buyCount }}</text>
          </div>
        </div>
      </div>
    </div>

    <div class="remark-card">
      <div class="row">
        <text class="label">买家留言</text>
        <input class="input" v-model="remark" placeholder="建议留言前先与卖家沟通确认" />
      </div>
    </div>

    <div class="submit-bar">
      <div class="price-box">
        <text class="label">合计:</text>
        <text class="total-price">¥{{ totalPrice }}</text>
      </div>
      <button class="submit-btn" @click="submitOrder" :disabled="submitting">
        {{ submitting ? '提交中...' : '提交订单' }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import request from '../../utils/request.js';

const productId = ref('');
const skuId = ref('');
const buyCount = ref(1);
const remark = ref('');
const submitting = ref(false);

// 页面展示数据
const skuInfo = ref({});
const productTitle = ref('');
const productPic = ref('');
const sellerName = ref('');

const totalPrice = computed(() => {
  if (!skuInfo.value.price) return '0.00';
  return (skuInfo.value.price * buyCount.value).toFixed(2);
});

onLoad((options) => {
  if (options.skuId) {
    skuId.value = options.skuId;
    productId.value = options.productId;
    buyCount.value = Number(options.count) || 1;
    loadPreOrderInfo();
  }
});

// 加载预览信息 (为了省事，我们直接重用详情接口来回显数据)
const loadPreOrderInfo = async () => {
  try {
    const res = await request({ url: `/app/product/detail/${productId.value}` });
    const data = res.data;
    productTitle.value = data.title;
    productPic.value = data.pic;
    sellerName.value = data.seller.nickname;
    
    // 找到当前选中的 SKU
    const targetSku = data.skuList.find(s => s.id == skuId.value);
    if (targetSku) {
      skuInfo.value = targetSku;
      // 如果SKU有图用SKU的图，没图用主图
      if (targetSku.skuPic) productPic.value = targetSku.skuPic;
    }
  } catch (e) {
    uni.showToast({ title: '商品信息加载失败', icon: 'none' });
  }
};

// ★★★ 核心：提交订单 ★★★
const submitOrder = async () => {
  submitting.value = true;
  try {
    // 调用后端 POST /app/order/create
    const res = await request({
      url: '/app/order/create',
      method: 'POST',
      data: {
        skuId: skuId.value,
        count: buyCount.value,
        addressId: 100, // 这里先写死Mock的地址ID，反正后端也没校验真实性
        remark: remark.value
      }
    });

    uni.showToast({ title: '下单成功', icon: 'success' });
    
    // 拿到订单号 res.data (根据你之前的后端日志，返回的是 orderNo 字符串)
    const orderNo = res.data;
    
    // 跳转到模拟支付页 (或者直接跳回首页，视情况而定)
    // 这里我们简单处理：直接弹窗提示支付
    uni.showModal({
      title: '模拟支付',
      content: `订单 ${orderNo} 创建成功！是否立即支付？`,
      success: async function (modalRes) {
        if (modalRes.confirm) {
          // 调用后端 Mock 支付接口
          await request({
            url: `/app/order/pay/mock?orderNo=${orderNo}`,
            method: 'POST'
          });
          uni.showToast({ title: '支付成功！' });
          setTimeout(() => {
            uni.switchTab({ url: '/pages/index/index' });
          }, 1500);
        } else {
          // 不支付，去首页
          uni.switchTab({ url: '/pages/index/index' });
        }
      }
    });

  } catch (e) {
    console.error(e);
  } finally {
    submitting.value = false;
  }
};
</script>

<style lang="scss">
.confirm-page { min-height: 100vh; background: $pin-bg-color; padding: 15px; padding-bottom: 80px; }

.address-card {
  background: #fff; padding: 20px; border-radius: 10px; margin-bottom: 15px; position: relative;
  .addr-top { display: flex; align-items: center; margin-bottom: 8px; }
  .tag { background: $pin-primary; color: #fff; font-size: 10px; padding: 1px 4px; border-radius: 2px; margin-right: 5px; }
  .city { font-size: 14px; color: $pin-text-regular; }
  .addr-detail { font-size: 18px; font-weight: bold; margin-bottom: 8px; color: $pin-text-main; }
  .user-info { font-size: 14px; color: $pin-text-regular; }
  .arrow { position: absolute; right: 15px; top: 50%; transform: translateY(-50%); color: #ccc; }
}

.product-card {
  background: #fff; border-radius: 10px; padding: 15px; margin-bottom: 15px;
  .shop-name { font-size: 14px; font-weight: bold; margin-bottom: 10px; border-bottom: 1px solid #f5f5f5; padding-bottom: 8px; }
  .goods-box { display: flex; }
  .img { width: 80px; height: 80px; background: #f0f0f0; border-radius: 4px; margin-right: 10px; }
  .info { flex: 1; display: flex; flex-direction: column; justify-content: space-between; }
  .title { font-size: 14px; color: $pin-text-main; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; width: 200px;}
  .sku-name { font-size: 12px; color: $pin-text-placeholder; background: #f8f8f8; padding: 2px 5px; border-radius: 4px; align-self: flex-start; }
  .price-row { display: flex; justify-content: space-between; align-items: center; }
  .price { color: $pin-danger; font-size: 16px; font-weight: bold; }
  .count { font-size: 14px; color: $pin-text-regular; }
}

.remark-card {
  background: #fff; border-radius: 10px; padding: 15px;
  .row { display: flex; align-items: center; }
  .label { font-size: 14px; width: 70px; }
  .input { flex: 1; font-size: 14px; text-align: right; }
}

.submit-bar {
  position: fixed; bottom: 0; left: 0; width: 100%; height: 60px; background: #fff; display: flex; align-items: center; justify-content: flex-end; padding: 0 15px; box-shadow: 0 -2px 10px rgba(0,0,0,0.05); box-sizing: border-box; z-index: 100;
  .price-box { display: flex; align-items: center; margin-right: 15px; }
  .label { font-size: 14px; color: #333; margin-right: 5px; }
  .total-price { font-size: 20px; color: $pin-danger; font-weight: bold; }
  .submit-btn {
    width: 120px; height: 40px; line-height: 40px; background: linear-gradient(135deg, $pin-primary, $pin-success); color: #fff; border-radius: 20px; font-size: 16px; font-weight: bold; margin: 0;
    &::after { border: none; }
    &:active { opacity: 0.9; }
  }
}
</style>