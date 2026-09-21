<!-- 相关问题 - 问答详情侧边栏 (IAIC 风格) -->
<template>
  <div class="relatedQuestions">
    <div class="but">
      <button class="iaic-btn-publish" @click="$router.push('/ask/index')">
        ✍️ 发新问题
      </button>
    </div>
    <div class="tit">🔥 社区热议讨论</div>
    <div class="related-list" v-loading="loading">
      <div
        v-for="item in relatedList"
        :key="item.id"
        class="related-item"
        @click="$router.push({ path: '/askDetails', query: { id: item.id } })"
      >
        <p class="r-title">{{ item.title }}</p>
        <span class="r-meta">{{ item.replyCount || item.replyTimes || 0 }} 回答 · {{ item.likeCount || 0 }} 赞</span>
      </div>
      <div v-if="!loading && relatedList.length === 0" class="empty-tip">
        暂无相关推荐
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { getAskList } from '@/api/classDetails.js';

const props = defineProps({
  id: { type: String, default: '' },
  title: { type: String, default: '' }
});

const loading = ref(false);
const relatedList = ref([]);

const loadRelated = async () => {
  loading.value = true;
  try {
    const res = await getAskList({ pageNo: 1, pageSize: 6 });
    if (res && res.code === 200 && res.data) {
      const list = res.data.list || res.data.records || [];
      relatedList.value = list.filter(item => String(item.id) !== String(props.id)).slice(0, 5);
    }
  } catch (e) {
    console.debug('Failed to load related questions:', e);
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  loadRelated();
});
</script>

<style lang="scss" scoped>
.relatedQuestions {
  width: 340px;
  background: var(--card);
  border: 1px solid var(--line);
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(19, 41, 79, 0.04);
  font-family: var(--cn);

  .but {
    border-bottom: 1px solid var(--line);
    padding-bottom: 20px;
    margin-bottom: 20px;
    text-align: center;
  }

  .iaic-btn-publish {
    width: 100%;
    height: 40px;
    border-radius: 20px;
    background: linear-gradient(90deg, #38b6ff, #2a8fff);
    color: #fff;
    font-size: 14px;
    font-weight: 700;
    border: none;
    cursor: pointer;
    box-shadow: 0 4px 14px rgba(42, 143, 255, 0.35);
    transition: all 0.2s;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 6px 18px rgba(42, 143, 255, 0.5);
    }
  }

  .tit {
    font-weight: 800;
    font-size: 16px;
    color: var(--ink);
    margin-bottom: 14px;
  }

  .related-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .related-item {
    cursor: pointer;
    padding: 8px 10px;
    border-radius: 8px;
    transition: all 0.2s;

    &:hover {
      background: var(--sky-2);
      .r-title { color: var(--azure); }
    }

    .r-title {
      font-size: 13.5px;
      font-weight: 600;
      color: var(--navy);
      line-height: 1.45;
      margin: 0 0 4px 0;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }

    .r-meta {
      font-size: 11.5px;
      color: var(--slate-2);
      font-family: var(--mono);
    }
  }

  .empty-tip {
    font-size: 12.5px;
    color: var(--slate);
    text-align: center;
    padding: 12px 0;
  }
}
</style>
