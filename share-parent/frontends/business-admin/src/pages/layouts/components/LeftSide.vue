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
  position: relative;
  position: fixed;
  overflow: hidden;
  z-index: 999;
  width: 226px;
  height: 100vh;
  background: linear-gradient(180deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);

  .logo{
    position: relative;
    z-index: 9;
    margin-top: 24px;
    margin-bottom: 32px;
    padding: 0 20px;
  }

  .logo-container {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .logo-icon {
    width: 44px;
    height: 44px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    font-size: 22px;
    font-weight: 700;
    box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
  }

  .logo-text {
    display: flex;
    flex-direction: column;
  }

  .logo-name {
    font-size: 18px;
    font-weight: 700;
    color: #fff;
    line-height: 1.2;
  }

  .logo-desc {
    font-size: 11px;
    color: rgba(255, 255, 255, 0.6);
    margin-top: 2px;
  }
  .title{
    position: relative;
    z-index: 9;
    text-align: center;
    line-height: 60px;
    font-size: 18px;
    margin-bottom: 15px;
    color: #fff;
  }
  .nav{
    position: relative;
    z-index: 9;
    font-size: 14px;
    margin-right: 20px;
    .navIcon{
      margin-right: 16px;
    }
    .iconfont{
      font-size: 20px;
      margin-right: 10px;
      color: rgba(255, 255, 255, 0.7);
    }
    .item{
      padding-left: 46px;
      padding-right: 43px;
      .navTopTit{
        display: flex;
        justify-content: space-between;
        padding: 10px 0;
        cursor: pointer;
        .vanIcon{
          width: 16px;
          height: 16px;
        }
      }
      .navListTit{
        // padding-left: 20px;
      }
    }
  }
  #svg{
    fill: rgba(255, 255, 255, 0.7);
  }
  .first-menu{
    height: 46px;
    line-height: 46px;
    .el-menu-item{
      padding-left: 46px !important;
      height: 46px;
      line-height: 46px;
      border-radius: 0 100px 100px 0;
      color: rgba(255, 255, 255, 0.7);
      &:hover{
        background-color: rgba(102, 126, 234, 0.3);
        color: #fff;
      }
    }
    .is-active{
      color:#fff;
      background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
      border-radius: 0 100px 100px 0;
      height: 46px;
      &:hover{
        color: #fff;
        background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
      }
    }
  }
  :deep(.el-sub-menu__title){
    padding-left: 46px !important;
    border-radius: 0 100px 100px 0;
    color: #fff !important;
    &:hover {
      background-color: rgba(102, 126, 234, 0.3);
      color: #fff !important;
    }
  }
  :deep(.el-sub-menu__icon-arrow){
    font-size: 15px;
    color: rgba(255, 255, 255, 0.8) !important;
  }
  :deep(.is-active .el-sub-menu__icon-arrow){
    font-size: 15px;
    color: #fff !important;
    font-weight: 600;
  }
  :deep(.is-active > .el-sub-menu__title){
    color:#fff !important;
    background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
    border-radius: 0 100px 100px 0;
    height: 46px;
  }
  :deep(.is-active > .el-sub-menu__title:hover){
    background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
    border-radius: 0 100px 100px 0;
    color:#fff !important;
  }
  :deep(.el-menu-item:hover, .el-sub-menu__title:hover){
    background: rgba(102, 126, 234, 0.3);
    color:#fff !important;
  }
  :deep(#svg){
    fill: rgba(255, 255, 255, 0.8);
  }
  :deep(.el-sub-menu__title:hover){
    background: rgba(102, 126, 234, 0.3);
    color:#fff !important;
  }
  :deep(.el-menu){
    background-color: transparent;
    border: none;
  }
  :deep(.el-menu-item){
    padding-left: 77px !important;
    color: rgba(255, 255, 255, 0.9) !important;
    &:hover {
      background-color: rgba(102, 126, 234, 0.3);
      color: #fff !important;
    }
  }
  :deep(.el-menu-item.is-active) {
    color: #fff !important;
    background: linear-gradient(90deg, rgba(102, 126, 234, 0.5) 0%, rgba(118, 75, 162, 0.5) 100%);
  }
}
</style>