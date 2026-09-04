<!-- 个人中心 - 左侧导航  -->
<template>
  <div class="LeftNav">
    <router-link class="fx-sb font-bt2" :class="{'active': activeClass(item)}" v-for="item in personalRoute" :key="item.name || item.path" :to="item.path">
      {{item.meta.title}}<i class="iconfont" v-html="item.meta.icon"></i>
    </router-link>
    <a  class="fx-sb font-bt2" @click="logout">退出<i class="iconfont">&#xe60c;</i></a>
  </div>
</template>
<script setup>
import { onMounted, ref, watchEffect, computed } from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {userLogout} from "@/api/user"
import { useUserStore } from '@/store'
const store = useUserStore();
const route = useRoute()
const router = useRouter()
// 使用路由
const personalRoute = ref([])
const currentPath = ref('')

const activeClass = computed(() => item => {
  const active = item.meta?.active || item.path.split('/').filter(Boolean).at(-1)
  return active ? currentPath.value === active || currentPath.value.startsWith(`${active}/`) : false
})

const loadPersonalRoutes = () => {
  const personalMain = router.currentRoute.value.matched.find(item => item.name === 'personalMain')
  const basePath = personalMain?.path || '/personal/main'
  personalRoute.value = (personalMain?.children || [])
    .filter(item => item.meta && !item.meta.hidden && item.path)
    .map(item => ({
      ...item,
      path: `${basePath.replace(/\/$/, '')}/${item.path}`,
    }))
}

onMounted(() => {
  // 获取个人中心实际内容路由的子路由作为导航，避免取到 /personal 的外层 children。
  loadPersonalRoutes()
  // 获取当前的路由作为默认项
  currentPath.value = route.path.split('/').at(-1);
})
// 通过监听实现 更新当前页面状态
watchEffect(() => {
  loadPersonalRoutes()
  currentPath.value = route.path.split('/').at(-1);
})

// 退出登录
const logout = () => {
  userLogout().then(res => {
    if (res.code === 200) {
      store.logout();
      location.href = "/"
    }
  }).catch(err => console.log(err))
}

</script>
<style lang="scss" scoped>
.LeftNav{
  width: 190px;
  height:fit-content;
  background: #FFFFFF;
  border-radius: 8px;
  margin-right: 20px;
  a{
    width: 100%;
    padding: 13px 15px 13px 20px;
    border-bottom: 1px solid #EEEEEE;
    font-size: 14px;
    line-height: 32px;
    i{
      font-size: 30px;
    }
  }
  .active{
    color: var(--color-main);
  }
}
</style>
