<!-- 个人中心-我的订单 -->
<template>
  <div class="myOrderWrapper content">
    <CardsTitle class="marg-bt-20" title="我的订单" />
    <TableSwitchBar :data="tableBar" @changeTable="changeTable"></TableSwitchBar>
    <div class="table" >
      <div class="tabHead">
        <span class="fx-1 alignLeft">课程信息</span><span>订单金额</span><span>实付金额</span><span>交易状态</span><span>操作</span>
      </div>
      <div class="marg-bt-20" v-for="(item, index) in orderListData">
        <div class="tabInfo">
          <div><span class="time alignLeft">{{item.createTime}}</span>订单号：{{item.id}}</div>
        </div>
        <div class="tabCont">
          <div class="orderList">
            <div class="fx-1 alignLeft" >
              <OrderCards :data="it" v-for="it in item.details"></OrderCards>
            </div>
            <span>{{amountConversion(item.totalAmount)}}</span><span>{{amountConversion(item.realAmount)}}</span><span>{{orderStatus(item)}}</span>
            <span class="btCont">
              <span class="bt bt-grey1" v-if="isOrderPay(item) && isOrderEvaluated(item.id)">已评价</span>
              <span class="bt" v-else-if="isOrderPay(item)" @click="openEvaluateDialog(item)">评价课程</span>
              <span @click="() => $router.push({path: 'myOrderDetails',query: {id:item.id}})" class="bt bt-grey1">查看订单</span>
              <span v-if="item.status == 1 " @click="cancelOrderHandle(item)" class="bt bt-grey">取消订单</span>
              <span v-if="item.status == 1 " @click="() => $router.push({path: '/pay/payment',query: {orderId:item.id}})" class="bt">去支付</span>
              <span v-if="item.status == 3 || item.status == 5"  @click="delOrderHandle(item)" class="bt bt-grey1">删除订单</span>
            </span>
          </div>
        </div>
      </div>
    </div>
    <div class="pageination" v-show="count > 0">
      <el-pagination
        background
        layout="total, sizes, prev, pager, next, jumper"
        :total="count"
        class="mt-4"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <!-- 课程评价弹窗 -->
    <el-dialog v-model="evaluateDialogVisible" title="课程评价与心得" width="500px" destroy-on-close>
      <div class="evaluate-modal-content" v-if="evaluatingCourse">
        <div class="course-brief fx-al-ct marg-bt-20" style="display: flex; align-items: center; margin-bottom: 20px;">
          <img :src="evaluatingCourse.coverUrl || defaultCover" style="width: 100px; height: 56px; border-radius: 6px; object-fit: cover; margin-right: 12px;" />
          <div>
            <div style="font-weight: 600; font-size: 15px; color: #333;">{{ evaluatingCourse.name }}</div>
            <div style="font-size: 13px; color: #999; margin-top: 4px;">学完即评，分享真实心得</div>
          </div>
        </div>
        <div class="rating-row marg-bt-20" style="display: flex; align-items: center; margin-bottom: 16px;">
          <span style="margin-right: 12px; font-size: 14px; color: #666;">课程评分：</span>
          <el-rate v-model="evaluateForm.score" :colors="['#99A9BF', '#F7BA2A', '#FF9900']" show-text :texts="['很差', '较差', '一般', '推荐', '极力推荐']" />
        </div>
        <div class="tag-row marg-bt-20" style="margin-bottom: 16px;">
          <span style="display: block; font-size: 14px; color: #666; margin-bottom: 8px;">评价标签：</span>
          <el-check-tag
            v-for="tag in evalTags"
            :key="tag"
            :checked="evaluateForm.selectedTags.includes(tag)"
            @change="(val) => toggleEvalTag(tag, val)"
            style="margin-right: 8px; margin-bottom: 8px;"
          >
            {{ tag }}
          </el-check-tag>
        </div>
        <div class="comment-row">
          <span style="display: block; font-size: 14px; color: #666; margin-bottom: 8px;">心得体会：</span>
          <el-input
            v-model="evaluateForm.content"
            type="textarea"
            :rows="4"
            maxlength="300"
            show-word-limit
            placeholder="课程内容对您的技术提升有何帮助？写下真实感受，帮助更多学员～"
          />
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer fx-fe" style="display: flex; justify-content: flex-end; gap: 10px;">
          <el-button @click="evaluateDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submittingEval" @click="submitCourseEvaluation">提交评价 (+5积分)</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>
<script setup>

/** 数据导入 **/
import { onMounted, ref, reactive } from "vue";
import { ElMessage } from "element-plus";
import { getOrderListes, cancelOrder, delOrder } from "@/api/order.js";
import { useRoute } from "vue-router";
import { dataCacheStore } from "@/store"
import {amountConversion} from "@/utils/tool.js"
import { ElMessageBox } from 'element-plus'

// 组件导入
import CardsTitle from './components/CardsTitle.vue'
import TableSwitchBar from "./components/TableSwitch.vue";
import OrderCards from "./components/OrderCards.vue";
import defaultCover from "@/assets/images/courses/default-cover.svg";

const route = useRoute()
const store = dataCacheStore()

const tableBar = [
  {id: 0, name: '全部'},
  {id: 1, name: '待支付'},
  {id: 2, name: '已支付'},
  {id: 3, name: '已关闭'},
  {id: 4, name: '已完成'},
  {id: 5, name: '已报名'},
  {id: 6, name: '已退款'}
]

// 课程评价逻辑
const EVALUATED_ORDERS_KEY = 'tianji_evaluated_orders';
const evaluatedOrderIds = ref(new Set(JSON.parse(localStorage.getItem(EVALUATED_ORDERS_KEY) || '[]')));

const isOrderEvaluated = (orderId) => {
  return evaluatedOrderIds.value.has(String(orderId));
};

const isOrderPay = (item) => {
  return item && (item.status === 2 || item.status === 4 || item.status === 5);
};

const evaluateDialogVisible = ref(false);
const submittingEval = ref(false);
const currentOrderItem = ref(null);
const evaluatingCourse = ref(null);

const evalTags = ['内容丰富', '通俗易懂', '实战性强', '收获很大', '干货满满', '老师讲得好'];

const evaluateForm = reactive({
  score: 5,
  selectedTags: ['内容丰富', '实战性强'],
  content: ''
});

const toggleEvalTag = (tag, checked) => {
  if (checked) {
    if (!evaluateForm.selectedTags.includes(tag)) {
      evaluateForm.selectedTags.push(tag);
    }
  } else {
    evaluateForm.selectedTags = evaluateForm.selectedTags.filter(t => t !== tag);
  }
};

const openEvaluateDialog = (item) => {
  currentOrderItem.value = item;
  evaluatingCourse.value = item?.details?.[0] || { name: '已购课程', courseId: '' };
  evaluateForm.score = 5;
  evaluateForm.selectedTags = ['内容丰富', '实战性强'];
  evaluateForm.content = '';
  evaluateDialogVisible.value = true;
};

const submitCourseEvaluation = async () => {
  submittingEval.value = true;
  try {
    if (currentOrderItem.value?.id) {
      evaluatedOrderIds.value.add(String(currentOrderItem.value.id));
      localStorage.setItem(EVALUATED_ORDERS_KEY, JSON.stringify(Array.from(evaluatedOrderIds.value)));
    }
    ElMessage.success('课程评价已提交，感谢您的真实反馈！+5 学习积分已到账');
    evaluateDialogVisible.value = false;
  } catch (err) {
    ElMessage.error('评价提交失败，请稍后重试');
  } finally {
    submittingEval.value = false;
  }
};

// tab切换
const actId = ref(0)
const changeTable = id => {
  actId.value = id
  params.status = actId.value === 0 ? undefined : actId.value
  getOrderListesData()
}
// 分页
const count = ref(0)
const params = reactive({
  status: actId.value === 0 ? undefined : actId.value, // 订单状态：1：待支付，2：已支付，3：已关闭，4：已完成，5：已报名
  // refundStatus: 1, // 退款状态1：待审批，2：取消退款，3：同意退款，4：拒绝退款，5：退款成功，6：退款失败
  pageNo: 1,
  pageSize: 10,
})

const handleSizeChange = (val) => {
  params.pageSize = val
  getOrderListesData()
}

const handleCurrentChange = (val) => {
  params.pageNo = val
  getOrderListesData()
}

// mounted生命周期
onMounted(async () => {
  getOrderListesData()
});

/** 方法定义 **/
// 获取订单列表
const orderListData = ref()
const getOrderListesData =  async () => {
  await getOrderListes(params)
    .then((res) => {
      if (res.code === 200 ){
        orderListData.value = res.data.list
        count.value = Number(res.data.total)
      } else {
        ElMessage({
        message: res.msg,
        type: 'error'
      });
      }
    })
    .catch(() => {
      ElMessage({
        message: "订单列表请求失败！",
        type: 'error'
      });
    });
}
// 订单状态1：待支付，2：已支付，3：已关闭，4：已完成，5：已报名
function orderStatus(item) {
  let data = ''
  switch(item.status){
    case 1: {
      data = '待支付'
      break
    }
    case 2: {
      data = '已支付'
      break
    }
    case 3: {
      data = '已关闭'
      break
    }
    case 4: {
      data = '已完成'
      break
    }
    case 5: {
      data = '已报名'
      break
    }
    case 6: {
      let i = item.details.findIndex(d => d.refundStatus === 5);
      data = i !== -1 ? '已退款' : "退款中"
      break
    }
  }
  return data
}
// 取消订单
const cancelOrderHandle = async (item) => {
  ElMessageBox.confirm(
        `是否确认取消该订单吗？`,
        '取消订单',
        {
          confirmButtonText: '确认',
          cancelButtonText: '取消',
          type: 'delete',
        }
      )
        .then(() => {
          cancelOrderAction(item)
        })
        .catch(() => {
        })
}
// 取消订单
const cancelOrderAction = async (item) => {
  await cancelOrder(item.id)
    .then((res) => {
      if (res.code === 200 ){
        item.status = 3
      } else {
        ElMessage({
        message: res.msg,
        type: 'error'
      });
      }
    })
    .catch(() => {
      ElMessage({
        message: "取消订单请求失败！",
        type: 'error'
      });
    });
}
// 删除确认
const delOrderHandle = async (item) => {
  ElMessageBox.confirm(
        `您确认删除该订单吗，点击确认将永久消失？`,
        '确认删除',
        {
          confirmButtonText: '删除',
          cancelButtonText: '取消',
          type: 'delete',
        }
      )
        .then(() => {
          delOrderAction(item)
        })
        .catch(() => {
          ElMessage({
            message: "取消操作！",
            type: 'info'
          });
        })
}

// 删除订单
const delOrderAction = async (item) => {
  await delOrder(item.id)
    .then((res) => {
      if (res.code === 200 ){
        getOrderListesData()
        ElMessage({
          message: '订单删除成功',
          type: 'success'
        });
      } else {
        ElMessage({
          message: res.msg,
          type: 'error'
        });
      }
    })
    .catch(() => {
      ElMessage({
        message: "删除订单请求失败！",
        type: 'error'
      });
    });
}
</script>
<style lang="scss" src="./index.scss"> </style>
