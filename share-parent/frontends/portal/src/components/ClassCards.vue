<!-- 课程展示卡片 - 首页、搜索页 -->
<template>
  <div class="classCards" @click="goDetails(data.id)">
    <div class="image-wrapper">
      <img :src="data.coverUrl || defaultCover" alt="课程封面" class="cover-img" loading="lazy" @error="handleImgError" />
      <span v-if="data.categoryName" class="category-badge">{{ data.categoryName }}</span>
    </div>
    <div class="card-content">
      <div class="title" :title="data.name" v-html="data.name"></div>

      <div class="meta-info">
        <span class="teacher" v-if="data.teacher">讲师：{{ data.teacher }}</span>
        <span class="sections" v-if="data.sections">共 {{ data.sections }} 节</span>
      </div>

      <div class="card-footer">
        <span class="learners">{{ data.sold || 0 }} 人在学</span>
        <div class="price-wrapper">
          <span v-if="Number(data.price) > 0" class="price">
            <small>¥</small>{{ (Number(data.price) / 100).toFixed(2) }}
          </span>
          <span v-else class="price-free">免费学习</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router';
import defaultCover from '@/assets/images/courses/default-cover.svg';

const router = useRouter();
const props = defineProps({
  data: {
    type: Object,
    default: () => ({})
  },
  type: {
    type: String,
    default: 'default'
  }
});

const handleImgError = (e) => {
  if (e?.target && e.target.src !== defaultCover) {
    e.target.src = defaultCover;
  }
};

const goDetails = id => {
  if (id) {
    router.push({ path: '/details', query: { id } });
  }
};
</script>

<style lang="scss" scoped>
.classCards {
  position: relative;
  background: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  box-shadow: 0 1px 3px 0 rgba(15, 23, 42, 0.04);
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);

  &:hover {
    border-color: #CBD5E1;
    box-shadow: 0 8px 18px -4px rgba(15, 23, 42, 0.09);
    transform: translateY(-2px);

    .image-wrapper .cover-img {
      transform: scale(1.03);
    }
    .title {
      color: var(--color-main);
    }
  }

  .image-wrapper {
    width: 100%;
    height: 156px;
    position: relative;
    overflow: hidden;
    background: #F1F5F9;

    .cover-img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      display: block;
      transition: transform 0.3s ease;
    }

    .category-badge {
      position: absolute;
      top: 10px;
      left: 10px;
      background: rgba(15, 23, 42, 0.7);
      backdrop-filter: blur(4px);
      color: #FFFFFF;
      font-size: 11px;
      font-weight: 500;
      padding: 2px 8px;
      border-radius: 4px;
    }
  }

  .card-content {
    padding: 14px;
    display: flex;
    flex-direction: column;
    flex: 1;

    .title {
      font-size: 14px;
      font-weight: 600;
      line-height: 1.45;
      color: #0F172A;
      margin-bottom: 8px;
      height: 40px;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
      text-overflow: ellipsis;
      transition: color 0.2s ease;

      :deep(em) {
        font-style: normal;
        color: var(--color-main);
        font-weight: 700;
      }
    }

    .meta-info {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 12px;
      color: #64748B;
      margin-bottom: 12px;

      .teacher {
        max-width: 120px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
      .sections {
        position: relative;
        padding-left: 8px;
        &::before {
          content: '•';
          position: absolute;
          left: 0;
          color: #CBD5E1;
        }
      }
    }

    .card-footer {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-top: auto;
      padding-top: 10px;
      border-top: 1px solid #F1F5F9;

      .learners {
        font-size: 12px;
        color: #94A3B8;
      }

      .price-wrapper {
        .price {
          font-size: 16px;
          font-weight: 700;
          color: #DC2626;
          small {
            font-size: 12px;
            font-weight: 600;
            margin-right: 1px;
          }
        }
        .price-free {
          font-size: 13px;
          font-weight: 600;
          color: #059669;
          background: #ECFDF5;
          padding: 2px 8px;
          border-radius: 4px;
        }
      }
    }
  }
}
</style>
