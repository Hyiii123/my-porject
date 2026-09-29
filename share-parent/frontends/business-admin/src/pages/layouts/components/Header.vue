<template>
  <header class="admin-top-header">
    <div class="admin-nav-scan"></div>
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
.admin-top-header {
  position: relative;
  background: rgba(10, 14, 24, 0.88);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 4px 20px -2px rgba(0, 0, 0, 0.5);
  color: #F8FAFC;
  z-index: 100;

  .admin-nav-scan {
    position: absolute;
    bottom: 0;
    left: 0;
    width: 100%;
    height: 1px;
    background: linear-gradient(90deg, transparent, rgba(56, 189, 248, 0.6), transparent);
    opacity: 0.8;
  }

  .headerInfo {
    height: 64px;
    align-items: center;
    padding: 0 24px;

    .textDefault1 {
      font-size: 13.5px;
      color: #94A3B8;
      cursor: pointer;
      font-weight: 500;
      transition: color 0.2s;

      &:hover {
        color: #38BDF8;
      }

      &.ft-cl-des {
        color: #FFFFFF;
        font-weight: 600;
      }
    }

    .line {
      margin: 0 10px;
      color: rgba(255, 255, 255, 0.2);
    }

    .headIcon {
      width: 34px;
      height: 34px;
      border-radius: 50%;
      border: 1.5px solid rgba(56, 189, 248, 0.4);
      margin-right: 10px;
    }

    a {
      display: flex;
      align-items: center;
      color: #F8FAFC;
      font-size: 13.5px;
      font-weight: 600;
      transition: color 0.2s;

      &:hover {
        color: #38BDF8;
      }
    }

    .vline {
      width: 1px;
      height: 16px;
      background: rgba(255, 255, 255, 0.15);
      margin: 0 16px;
    }

    .back {
      cursor: pointer;
      opacity: 0.7;
      transition: opacity 0.2s;
      &:hover {
        opacity: 1;
      }
    }
  }
}
</style>