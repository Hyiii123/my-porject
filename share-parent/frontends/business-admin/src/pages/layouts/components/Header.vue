<template>
  <header class="bg-wt">
    <div class="fx headerInfo">
      <div class="fx-1 marg-lt-20">
        <div class="fx" v-show="route.meta.title != '首页'">
          <!-- <span class="textDefault1" @click="() => $router.push('/')">
            首页
          </span> -->
          <!-- <span class="line"> / </span> -->
          <span
            class="textDefault1"
            @click="() => $router.push({ path: route.matched[0].path })"
            >{{ route.matched[0].meta.title }}</span
          >
          <span class="line" > / </span>
          <span
            v-if="route.meta && route.meta.fmeta"
            class="textDefault1"
            @click="() => $router.push({ path: route.meta.fmeta.path })"
            >{{ route.meta.fmeta.title }}</span
          >
          <span v-if="route.meta && route.meta.fmeta" class="line" > / </span>
          <span
            class="textDefault1 ft-cl-des"
            @click="() => $router.push({ path: route.matched[1].path })"
            >{{ route.matched[1].meta.title }}</span
          >
        </div>
        <div class="fx" v-show="route.meta.title == '首页'">
          <span class="textDefault1" @click="() => $router.push('/')">
            工作台
          </span>
        </div>
      </div>
      <div class="fx-al-ct">
        <div v-if="$route.path == '/main/index'" class="wecom">
          <img src="@/assets/wecom-temp.png" width="268" height="22" alt="">
        </div>
        <div class="fx-al-ct" v-if="isToken && userInfo">
          <router-link to="/my/index">
            <img
              class="headIcon"
              :src="userInfo.icon"
              :onerror="onerrorImg"
              alt=""
            />
            <div>{{ userInfo.name || "admin" }}</div>
          </router-link>
          <span class="vline"></span>
          <div class="back" @click="goLogin()">
            <img src="@/assets/out.png" alt="" style="width:19px;height: 15px;" class="out"/>
          </div>
        </div>
      </div>
    </div>
  </header>
</template>
<script setup>
import { onMounted, ref, nextTick, watchEffect } from "vue";
import defaultImage from "@/assets/icon.jpeg";
import { useUserStore } from "@/store";
import router from "@/router";
import { useRoute } from "vue-router";
import { log } from "debug/src/browser"
import {TOKEN_NAME} from "@/config/global"
const store = useUserStore();
const userInfo = ref({});
const isToken = ref(false);
const route = useRoute();

onMounted(() => {
  isToken.value = sessionStorage.getItem(TOKEN_NAME) ? true : false;
});

watchEffect(() => {
  userInfo.value = store.getUserInfo;
});

const goLogin = () => {
  router.push("/login");
};
// 默认头像
const onerrorImg = () => {
  userInfo.value.icon = defaultImage;
};
</script>
<style lang="scss" scoped>
header {
  position: fixed;
  top: 0;
  left: 226px;
  z-index: 998;
  width: calc(100% - 226px);
  background-color: #ffffff;
  border-bottom: 1px solid #e2e8f0;
  text-align: left;
  padding: 0 28px;
  height: 60px;
  display: flex;
  align-items: center;
  font-size: 14px;
  box-sizing: border-box;

  .wecom {
    margin: 0 16px 0 0;
  }
  .headerInfo {
    width: 100%;
    display: flex;
    justify-content: space-between;
    align-items: center;

    .line {
      padding: 0 8px;
      color: #94a3b8;
    }
    .textDefault1 {
      color: #475569;
      font-weight: 500;
      cursor: pointer;
      transition: color 0.2s;
      &:hover {
        color: #2563eb;
      }
    }
    .ft-cl-des {
      color: #0f172a;
      font-weight: 600;
      cursor: default;
    }
  }
  .headIcon {
    width: 32px;
    height: 32px;
    border-radius: 50%;
    margin-right: 10px;
    object-fit: cover;
    border: 1px solid #e2e8f0;
    transition: opacity 0.2s;
    &:hover {
      opacity: 0.85;
    }
  }
  .vline {
    margin: 0 16px;
    background: #e2e8f0;
    width: 1px;
    height: 18px;
  }
  .back {
    position: relative;
    cursor: pointer;
    padding: 6px;
    border-radius: 6px;
    display: flex;
    align-items: center;
    transition: background-color 0.2s;
    &:hover {
      background-color: #f1f5f9;
    }
    img {
      width: 16px;
      height: 16px;
    }
  }
}
.fx-al-ct {
  display: flex;
  align-items: center;
  a {
    display: flex;
    align-items: center;
    color: #1e293b;
    font-weight: 500;
    text-decoration: none;
    &:hover {
      color: #2563eb;
    }
  }
}
</style>
