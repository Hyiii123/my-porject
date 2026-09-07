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
          defaultOpeneds.value == [index.toString()] ? null : defaultOpeneds.value = [index.toString()]
          item.children.forEach((val, ind) => {
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
        defaultOpeneds.value == [index.toString()] ? null : defaultOpeneds.value = [index.toString()]
        item.children.forEach((val, ind) => {
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
  background-color: #0F172A;
  border-right: 1px solid rgba(255, 255, 255, 0.06);

  &::-webkit-scrollbar {
    width: 4px;
  }
  &::-webkit-scrollbar-thumb {
    background: rgba(255, 255, 255, 0.15);
    border-radius: 2px;
  }

  .logo {
    position: relative;
    z-index: 9;
    padding: 20px 16px;
    margin-bottom: 12px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  }

  .logo-container {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  .logo-icon {
    width: 36px;
    height: 36px;
    background: #2563EB;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    font-size: 18px;
    font-weight: 700;
    box-shadow: 0 2px 8px rgba(37, 99, 235, 0.35);
  }

  .logo-text {
    display: flex;
    flex-direction: column;
  }

  .logo-name {
    font-size: 16px;
    font-weight: 700;
    color: #F8FAFC;
    line-height: 1.2;
    letter-spacing: 0.5px;
  }

  .logo-desc {
    font-size: 11px;
    color: #94A3B8;
    margin-top: 2px;
  }

  .nav {
    position: relative;
    z-index: 9;
    font-size: 14px;
    padding-bottom: 24px;

    .iconfont {
      font-size: 18px;
      margin-right: 10px;
      color: #94A3B8;
    }
  }

  .first-menu {
    margin: 3px 12px;
    .el-menu-item {
      height: 42px;
      line-height: 42px;
      padding-left: 16px !important;
      border-radius: 6px;
      color: #94A3B8;
      transition: all 0.2s ease;

      &:hover {
        background-color: rgba(255, 255, 255, 0.06);
        color: #F8FAFC;
      }
    }
    .is-active {
      color: #60A5FA !important;
      background-color: rgba(37, 99, 235, 0.16) !important;
      font-weight: 600;
      border-left: 3px solid #2563EB;
      &:hover {
        color: #60A5FA !important;
        background-color: rgba(37, 99, 235, 0.22) !important;
      }
    }
  }

  :deep(.el-menu) {
    background-color: transparent;
    border: none;
  }

  :deep(.el-sub-menu__title) {
    height: 42px;
    line-height: 42px;
    margin: 3px 12px;
    padding-left: 16px !important;
    border-radius: 6px;
    color: #CBD5E1 !important;
    transition: all 0.2s ease;

    &:hover {
      background-color: rgba(255, 255, 255, 0.06);
      color: #F8FAFC !important;
    }
  }

  :deep(.el-sub-menu__icon-arrow) {
    font-size: 12px;
    color: #64748B !important;
    right: 14px;
  }

  :deep(.is-active .el-sub-menu__icon-arrow) {
    color: #94A3B8 !important;
  }

  :deep(.is-active > .el-sub-menu__title) {
    color: #F8FAFC !important;
    font-weight: 600;
  }

  :deep(.el-menu-item) {
    height: 38px;
    line-height: 38px;
    margin: 2px 12px;
    padding-left: 44px !important;
    border-radius: 6px;
    color: #94A3B8 !important;
    font-size: 13px;
    transition: all 0.2s ease;

    &:hover {
      background-color: rgba(255, 255, 255, 0.06);
      color: #F8FAFC !important;
    }
  }

  :deep(.el-menu-item.is-active) {
    color: #60A5FA !important;
    background-color: rgba(37, 99, 235, 0.16) !important;
    font-weight: 600;
    border-left: 3px solid #2563EB;
  }
}
</style>