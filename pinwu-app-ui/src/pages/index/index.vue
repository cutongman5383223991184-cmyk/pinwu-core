<template>
  <div class="page-container">
    <div class="search-bar">
      <div class="input-box">
        <text class="icon">🔍</text>
        <input v-model="keyword" confirm-type="search" @confirm="loadData" class="input" placeholder="搜搜看：路飞 / 高数" />
      </div>
    </div>

    <swiper class="banner" indicator-dots autoplay circular interval="3000">
      <swiper-item><div class="banner-item bg-1">毕业季 · 教材大甩卖</div></swiper-item>
      <swiper-item><div class="banner-item bg-2">二次元 · 手办回血专场</div></swiper-item>
    </swiper>

    <div class="section-title">最新发布</div>
    <div class="product-list">
      <div class="product-card" v-for="(item, index) in list" :key="index" @click="goDetail(item.id)">
        <image class="img" :src="item.pic" mode="aspectFill"></image>
        <div class="info">
          <div class="title">{{ item.title }}</div>
          <div class="tags">
            <text class="tag" v-for="(tag, ti) in item.tags" :key="ti">{{ tag }}</text>
          </div>
          <div class="price-row">
            <text class="price">¥ {{ item.price }}</text>
            <text class="area">{{ item.city }}·{{ item.region }}</text>
          </div>
        </div>
      </div>
    </div>
    
    <div class="loading-text" v-if="loading">加载中...</div>
    <div class="empty-text" v-if="!loading && list.length === 0">暂无数据，快去发布吧</div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import request from '../../utils/request.js';

const keyword = ref('');
const list = ref([]);
const loading = ref(false);

// 加载数据 (调用后端 LBS 搜索接口)
const loadData = async () => {
  loading.value = true;
  try {
    const res = await request({
      url: '/app/product/search', // 记得后端是POST
      method: 'POST',
      data: {
        keyword: keyword.value,
        latitude: 32.2060,  // Mock: 假设我在南信大
        longitude: 118.7160
      }
    });
    list.value = res.data || [];
  } catch (e) {
    console.error(e);
  } finally {
    loading.value = false;
  }
};

// 跳转详情页 (等会儿再写详情页)
const goDetail = (id) => {
  uni.navigateTo({ url: '/pages/detail/detail?id=' + id });
};

// 页面加载时触发
onMounted(() => {
  loadData();
});
</script>

<style lang="scss">
.page-container { background-color: $pin-bg-color; min-height: 100vh; padding-bottom: 20px;}

.search-bar {
  position: sticky; top: 0; z-index: 100;
  padding: 10px 15px; background-color: $pin-primary;
  .input-box { 
    background: #fff; height: 36px; border-radius: 18px; display: flex; align-items: center; padding: 0 15px;
    .icon { margin-right: 5px; font-size: 14px; }
    .input { flex: 1; font-size: 14px; color: $pin-text-main; }
  }
}

.banner {
  height: 140px; margin: 15px; border-radius: 10px; overflow: hidden;
  .banner-item { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 20px; font-weight: bold; letter-spacing: 2px;}
  .bg-1 { background: linear-gradient(45deg, #6a11cb, #2575fc); }
  .bg-2 { background: linear-gradient(45deg, #ff9a9e, #fad0c4); }
}

.section-title { padding: 0 15px 10px; font-weight: bold; font-size: 16px; color: $pin-text-main; }

.product-list {
  padding: 0 10px; display: flex; flex-wrap: wrap; justify-content: space-between;
  .product-card {
    width: 48%; background: #fff; border-radius: 8px; margin-bottom: 10px; overflow: hidden; box-shadow: 0 2px 5px rgba(0,0,0,0.02);
    .img { width: 100%; height: 160px; background: #f0f0f0; }
    .info { padding: 10px; }
    .title { font-size: 14px; color: $pin-text-main; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-weight: 500;}
    .tags { display: flex; flex-wrap: wrap; margin: 5px 0; height: 18px; overflow: hidden;}
    .tag { font-size: 10px; color: $pin-primary; background: rgba(39,186,155,0.1); padding: 1px 4px; border-radius: 4px; margin-right: 4px; }
    .price-row { display: flex; justify-content: space-between; align-items: flex-end; margin-top: 5px; }
    .price { color: $pin-danger; font-size: 16px; font-weight: bold; }
    .area { color: $pin-text-placeholder; font-size: 10px; }
  }
}
.loading-text, .empty-text { text-align: center; color: #999; padding: 20px; font-size: 14px; }
</style>