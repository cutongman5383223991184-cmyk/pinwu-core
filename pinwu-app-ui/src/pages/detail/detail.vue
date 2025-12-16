<template>
  <div class="detail-page" v-if="info && info.id">
    <swiper class="swiper" indicator-dots autoplay circular interval="3000">
      <swiper-item v-if="albumList.length > 0" v-for="(img, index) in albumList" :key="index">
        <image class="slide-image" :src="img" mode="aspectFill" @click="previewImage(index)" />
      </swiper-item>
      <swiper-item v-else>
        <image class="slide-image" :src="info.pic" mode="aspectFill" />
      </swiper-item>
    </swiper>

    <div class="info-card">
      <div class="price-row">
        <div class="left">
          <text class="symbol">¥</text>
          <text class="price">{{ currentSku.price || info.price }}</text>
          <text class="original" v-if="info.originalPrice">¥{{ info.originalPrice }}</text>
        </div>
        <div class="view-count">{{ info.viewCount || 0 }}人围观</div>
      </div>
      <div class="title">{{ info.title }}</div>
      <div class="desc">{{ info.detail }}</div>
      <div class="tags">
        <text class="tag" v-for="(t, i) in info.tags" :key="i"># {{ t }}</text>
      </div>
      <div class="location">
        <text>📍 {{ info.city }} · {{ info.region }} · {{ info.locationName || '未知位置' }}</text>
      </div>
    </div>

    <div class="sku-card" v-if="skuList.length > 0">
      <div class="card-title">选择规格</div>
      <div class="sku-list">
        <div 
          class="sku-item" 
          :class="{ active: currentSku && currentSku.id === item.id }"
          v-for="(item, index) in skuList" 
          :key="index"
          @click="selectSku(item)"
        >
          {{ item.skuName }}
        </div>
      </div>
      
      <div class="count-row">
        <div class="label">购买数量 <text class="stock-tip">(库存: {{ currentSku.stock || 0 }})</text></div>
        <div class="stepper">
          <div class="btn minus" :class="{ disabled: buyCount <= 1 }" @click="changeCount(-1)">-</div>
          <div class="num">{{ buyCount }}</div>
          <div class="btn plus" :class="{ disabled: buyCount >= currentSku.stock }" @click="changeCount(1)">+</div>
        </div>
      </div>

      <div class="selected-tip">
        已选: {{ currentSku.skuName || '默认' }} x {{ buyCount }}
      </div>
    </div>

    <div class="seller-card">
      <div class="seller-top">
        <image class="avatar" :src="seller.avatar || '/static/logo.png'"></image>
        <div class="seller-info">
          <div class="nick">{{ seller.nickname || '神秘卖家' }}</div>
          <div class="credit">信用极好 | {{ seller.activeTimeText || '近期活跃' }}</div>
        </div>
      </div>
      <div class="contact-box">
        <div class="contact-text">
          联系方式: <text class="highlight">{{ realContact || seller.contactValue || '登录可见' }}</text>
        </div>
        <div class="contact-btn" @click="handleCheckContact" v-if="!realContact">
          点击查看
        </div>
      </div>
    </div>

    <div class="bottom-bar">
      <div class="btn-box">
        <div class="icon-btn">
          <text>💬</text>
          <text>私聊</text>
        </div>
        <button class="buy-btn" @click="handleBuy">立即购买</button>
      </div>
    </div>
  </div>
  
  <div v-else class="loading-page">加载中...</div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { onLoad ,onShow} from '@dcloudio/uni-app';
import request from '../../utils/request.js';

const productId = ref(null);
const info = ref({});
const seller = ref({});
const skuList = ref([]);
const currentSku = ref({});
const realContact = ref('');
// ★ 新增：购买数量状态
const buyCount = ref(1);

const albumList = computed(() => {
  if (info.value.albumPics) return info.value.albumPics.split(',');
  return info.value.pic ? [info.value.pic] : [];
});

onLoad((options) => {
  if (options.id) {
    productId.value = options.id;
  }
});

onShow(() => {
  // 只有拿到ID了才去查，防止报错
  if (productId.value) {
    getDetail(); 
  }
});

const getDetail = async () => {
  try {
    const res = await request({ url: `/app/product/detail/${productId.value}` });
    console.log('【详情接口原始返回】', JSON.stringify(res.data));
    info.value = res.data || {};
    seller.value = res.data.seller || {};
    skuList.value = res.data.skuList || [];
    
    if (skuList.value.length > 0) {
      currentSku.value = skuList.value[0];
      console.log('【当前SKU】', JSON.stringify(currentSku.value));
      // 切换商品时重置数量
      buyCount.value = 1;
    }
  } catch (e) { console.error(e); }
};

// 切换规格
const selectSku = (item) => {
  currentSku.value = item;
  // 切换规格重置为1，防止上个规格库存大，切过来后超出新规格库存
  buyCount.value = 1; 
};

// ★ 新增：修改数量逻辑
const changeCount = (step) => {
  const newCount = buyCount.value + step;
  const maxStock = currentSku.value.stock || 0;

  // 最小值限制
  if (newCount < 1) return;
  
  // 最大值限制 (库存)
  if (newCount > maxStock) {
    return uni.showToast({ title: '库存不足啦', icon: 'none' });
  }

  buyCount.value = newCount;
};

const handleCheckContact = async () => {
  try {
    const res = await request({
      url: `/app/product/contact/${productId.value}`,
      method: 'GET'
    });
    realContact.value = res.data;
    uni.showToast({ title: '已获取联系方式', icon: 'success' });
  } catch (e) {}
};

// 立即购买
const handleBuy = () => {
  if (!currentSku.value.id) return uni.showToast({ title: '请选择规格', icon: 'none' });
  if (currentSku.value.stock <= 0) return uni.showToast({ title: '该规格已售罄', icon: 'none' });

  // ★ 将 buyCount.value 传给下一个页面
  const url = `/pages/order/confirm?productId=${productId.value}&skuId=${currentSku.value.id}&count=${buyCount.value}`;
  uni.navigateTo({ url });
};

const previewImage = (current) => {
  uni.previewImage({ current: albumList.value[current], urls: albumList.value });
};
</script>

<style lang="scss">
.detail-page { padding-bottom: 80px; background: $pin-bg-color; min-height: 100vh; }
.loading-page { text-align: center; padding-top: 100px; color: #999; }

.swiper { width: 100%; height: 350px; background: #fff; .slide-image { width: 100%; height: 100%; }}

/* 信息卡片 */
.info-card {
  background: #fff; padding: 15px; margin-bottom: 10px;
  .price-row { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 10px; .left { color: $pin-danger; font-weight: bold; } .symbol { font-size: 14px; } .price { font-size: 24px; margin-right: 8px; } .original { color: #999; text-decoration: line-through; font-size: 12px; } .view-count { font-size: 12px; color: #999; }}
  .title { font-size: 18px; font-weight: bold; color: $pin-text-main; margin-bottom: 8px; line-height: 1.4; }
  .desc { font-size: 14px; color: $pin-text-regular; line-height: 1.6; margin-bottom: 10px; }
  .tags { display: flex; flex-wrap: wrap; margin-bottom: 10px; .tag { background: #f0f9eb; color: $pin-primary; font-size: 12px; padding: 2px 8px; border-radius: 4px; margin-right: 8px; } }
  .location { font-size: 12px; color: #999; border-top: 1px solid #f5f5f5; padding-top: 10px; }
}

/* 规格与数量卡片 */
.sku-card {
  background: #fff; padding: 15px; margin-bottom: 10px;
  .card-title { font-size: 15px; font-weight: bold; margin-bottom: 10px; }
  
  .sku-list { display: flex; flex-wrap: wrap; margin-bottom: 15px; }
  .sku-item {
    background: #f5f5f5; color: $pin-text-main; font-size: 13px; padding: 6px 15px; border-radius: 20px; margin-right: 10px; margin-bottom: 10px; border: 1px solid transparent;
    &.active { background: rgba(39,186,155,0.1); color: $pin-primary; border-color: $pin-primary; }
  }

  /* ★ 新增：数量选择器样式 ★ */
  .count-row {
    display: flex; justify-content: space-between; align-items: center; border-top: 1px solid #f9f9f9; padding-top: 15px;
    .label { font-size: 14px; font-weight: bold; color: $pin-text-main; }
    .stock-tip { font-weight: normal; color: #999; font-size: 12px; margin-left: 5px; }
    
    .stepper {
      display: flex; align-items: center;
      .btn {
        width: 32px; height: 32px; background: #f5f5f5; border-radius: 4px; display: flex; align-items: center; justify-content: center; font-size: 18px; color: #333; font-weight: bold;
        &.disabled { color: #ddd; background: #fcfcfc; } /* 禁用状态 */
        &:active:not(.disabled) { background: #e0e0e0; }
      }
      .num { width: 40px; text-align: center; font-size: 14px; font-weight: bold; }
    }
  }

  .selected-tip { font-size: 12px; color: #999; margin-top: 15px; }
}

/* 卖家卡片 */
.seller-card {
  background: #fff; padding: 15px; margin-bottom: 10px;
  .seller-top { display: flex; align-items: center; margin-bottom: 15px; }
  .avatar { width: 50px; height: 50px; border-radius: 50%; background: #eee; margin-right: 10px; }
  .nick { font-size: 16px; font-weight: bold; }
  .credit { font-size: 12px; color: #999; margin-top: 4px; }
  .contact-box { background: #f9f9f9; padding: 10px; border-radius: 8px; display: flex; justify-content: space-between; align-items: center; .contact-text { font-size: 14px; color: #666; } .highlight { color: $pin-text-main; font-weight: bold; margin-left: 5px; } .contact-btn { font-size: 12px; color: $pin-primary; border: 1px solid $pin-primary; padding: 4px 10px; border-radius: 15px; }}
}

.bottom-bar {
  position: fixed; bottom: 0; left: 0; width: 100%; height: 60px; background: #fff; display: flex; align-items: center; padding: 0 15px; box-shadow: 0 -2px 10px rgba(0,0,0,0.05); z-index: 100;
  .btn-box { width: 100%; display: flex; align-items: center; }
  .icon-btn { display: flex; flex-direction: column; align-items: center; margin-right: 20px; font-size: 10px; color: #666; text:first-child { font-size: 20px; margin-bottom: 2px; } }
  .buy-btn { flex: 1; height: 40px; line-height: 40px; background: linear-gradient(135deg, $pin-primary, $pin-success); color: #fff; border-radius: 20px; font-size: 16px; font-weight: bold; border:none;}
}
</style>