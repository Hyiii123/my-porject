<!-- 架构页面 - 左侧导航 -->
<template>
  <div class="LeftSider fx-fd-col">
    <div @click="() => $router.push('/')" class="logo cursor fx-ct">
      <div class="logo-container">
        <div class="logo-icon">智</div>
        <div class="logo-text">
          <span class="logo-name">智问学伴</span>
          <span class="logo-desc">管理后台</span>
        </div>
      </div>
    </div>
    <div class="nav">
      <el-menu
        :default-active="activeIndex"
        :default-openeds="defaultOpeneds"
        class="el-menu-vertical-demo"
        :unique-opened="true"
        @open="handleOpen"
        @close="handleClose"
        @select="handleSelect"
      > 
        <div class="first-menu">
          <el-menu-item index="99" :key="99" @click="goPath(`/`)">
            <i class="iconfont" v-html="basePath[0].meta.icon"></i>
            <span>工作台</span>
          </el-menu-item>
        </div> 
        <el-sub-menu v-for="(item, index) in basePath"  :key="index"  :index="index.toString()" >
          <template #title>
            <i class="iconfont" v-html="item.meta.icon"></i>
            <span>{{item.meta.title}}</span>
          </template>
          <el-menu-item v-for="(it, ind) in item.children" :key="ind" :index="`${index}-${ind}`" @click="goPath(`${it.path}`)">
            {{it.meta.title}}
          </el-menu-item>
        </el-sub-menu>
      </el-menu>
    </div>
    <span class="decorate"></span>
  </div>
</template>
<script setup>
import { ref, computed, watchEffect, } from 'vue';
import { useRoute } from 'vue-router';
import router, { asyncRouterList } from '@/router';
import { catchDataesStore,useUserStore } from '@/store';
// 全部路由信息
const routers = asyncRouterList

// 当前路由下的信息
const route = useRoute()

const store = catchDataesStore()
const useStore =useUserStore()

const activeIndex = ref('99')// ref(store.getDefaultIndex)
const defaultOpeneds = ref()// ref(store.getDefaultOpeneds)

// 处理侧边栏数据
const sideMenu = computed(() => {
  const newMenuRouters = [];
  routers.forEach((menu) => {
  })
  return newMenuRouters;
});

// 处理导航数据
const getMenuList = (list, basePath) => {
  if (!list) {
    return [];
  }
  return list
    .map((item) => {
      const path = basePath ? `${basePath}/${item.path}` : item.path;
      return {
        path,
        title: item.meta?.title,
        icon: item.meta?.icon || '',
        children: getMenuList(item.children, path),
        meta: item.meta,
        redirect: item.redirect,
      };
    })
    .filter((item) => item.meta && item.meta.hidden !== true);
};
// 展示基础路由
const basePath = getMenuList(routers)
// 进入导航
const goPath = (path) => {
  useStore.setTabNumber(0)
  router.push(path)
}
const handleOpen = (key) => {
  store.setDefaultOpeneds(key)
}
const handleClose = (key) => {
  store.setDefaultOpeneds(key)
}
const handleSelect = (key) => {
  store.setDefaultIndex(key)
}
// 处理  页面点击 菜单跟着动
watchEffect(()=>{
  if (basePath){
    const path = route.path.toString()
    // 如果是首页
    if(path == '/main/index' || path == '/'){
        activeIndex.value = '99'
        return 
      }
    // 如果是 三级子页  
    if(route.meta && route.meta.fmeta){
      const cpath = route.meta.fmeta.path
      basePath.forEach((item,index) => {
        const regA = new RegExp(item.path)
        // 非首页 在当前的路由下查找 
        if (cpath.search(regA) != -1){
          defaultOpeneds.value = [index.toString()];
          (item.children || []).forEach((val, ind) => {
            if (val.path == cpath){
              activeIndex.value == `${index}-${ind}` ? null : activeIndex.value = `${index}-${ind}`
            } 
          })
        }
      })
      return ;
    }
    // 非首页的二级页面
    basePath.forEach((item,index) => {
      const regA = new RegExp(item.path)
      // 非首页 在当前的路由下查找 
      if (path.search(regA) != -1){
        defaultOpeneds.value = [index.toString()];
        (item.children || []).forEach((val, ind) => {
          if (val.path == route.path){
            activeIndex.value == `${index}-${ind}` ? null : activeIndex.value = `${index}-${ind}`
          } 
        })
      }
    })
  }
})
</script>
<style lang="scss" scoped>
.LeftSider {
  position: fixed;
  top: 0;
  left: 0;
  overflow-y: auto;
  overflow-x: hidden;
  z-index: 999;
  width: 226px;
  height: 100vh;
  background-color: #FFFFFF;
  border-right: 1px solid #E2E8F0;
  box-shadow: 1px 0 12px rgba(15, 23, 42, 0.02);

  &::-webkit-scrollbar {
    width: 4px;
  }
  &::-webkit-scrollbar-thumb {
    background: #E2E8F0;
    border-radius: 2px;
  }

  .logo {
    position: relative;
    z-index: 9;
    padding: 18px 16px;
    margin-bottom: 8px;
    border-bottom: 1px solid #F1F5F9;
  }

  .logo-container {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  .logo-icon {
    width: 34px;
    height: 34px;
    background: linear-gradient(135deg, #2563EB 0%, #1D4ED8 100%);
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    font-size: 17px;
    font-weight: 700;
    box-shadow: 0 4px 10px rgba(37, 99, 235, 0.25);
  }

  .logo-text {
    display: flex;
    flex-direction: column;
  }

  .logo-name {
    font-size: 15px;
    font-weight: 700;
    color: #0F172A;
    line-height: 1.2;
    letter-spacing: 0.3px;
  }

  .logo-desc {
    font-size: 11px;
    color: #64748B;
    margin-top: 2px;
  }

  .nav {
    position: relative;
    z-index: 9;
    font-size: 13.5px;
    padding-bottom: 24px;

    .iconfont {
      font-size: 16px;
      margin-right: 10px;
      color: #64748B;
      transition: color 0.2s;
    }
  }

  .first-menu {
    margin: 4px 10px;
    .el-menu-item {
      height: 40px;
      line-height: 40px;
      padding-left: 14px !important;
      border-radius: 8px;
      color: #334155;
      font-weight: 500;
      transition: all 0.2s ease;

      &:hover {
        background-color: #F8FAFC;
        color: #0F172A;
        .iconfont { color: #2563EB; }
      }
    }
    .is-active {
      color: #2563EB !important;
      background: rgba(37, 99, 235, 0.08) !important;
      font-weight: 600;
      border-left: 3px solid #2563EB;
      .iconfont { color: #2563EB !important; }
      &:hover {
        background: rgba(37, 99, 235, 0.12) !important;
      }
    }
  }

  :deep(.el-menu) {
    background-color: transparent;
    border: none;
  }

  :deep(.el-sub-menu__title) {
    height: 40px;
    line-height: 40px;
    margin: 3px 10px;
    padding-left: 14px !important;
    border-radius: 8px;
    color: #334155 !important;
    font-weight: 500;
    transition: all 0.2s ease;

    &:hover {
      background-color: #F8FAFC;
      color: #0F172A !important;
      .iconfont { color: #2563EB; }
    }
  }

  :deep(.el-sub-menu__icon-arrow) {
    font-size: 12px;
    color: #94A3B8 !important;
    right: 14px;
  }

  :deep(.is-active .el-sub-menu__icon-arrow) {
    color: #2563EB !important;
  }

  :deep(.is-active > .el-sub-menu__title) {
    color: #0F172A !important;
    font-weight: 600;
    .iconfont { color: #2563EB !important; }
  }

  :deep(.el-menu-item) {
    height: 36px;
    line-height: 36px;
    margin: 2px 10px;
    padding-left: 42px !important;
    border-radius: 8px;
    color: #64748B !important;
    font-size: 13px;
    font-weight: 500;
    transition: all 0.2s ease;

    &:hover {
      background-color: #F8FAFC;
      color: #0F172A !important;
    }
  }

  :deep(.el-menu-item.is-active) {
    color: #2563EB !important;
    background-color: rgba(37, 99, 235, 0.08) !important;
    font-weight: 600;
    border-left: 3px solid #2563EB;
  }
}
</style>
