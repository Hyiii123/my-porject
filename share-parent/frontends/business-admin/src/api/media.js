import request from "@/utils/request.js";
// 获取列表
export const getMedia = (params) =>
  request({
    url: `/ms/medias`,
    method: "get",
    params,
  });
// 删除媒资
export const deleteMedia = (id) =>
  request({
    url: `/ms/medias/${id}`,
    method: "delete",
  });
// 批量删除媒资视频
export const deleteMediaAll = (params) =>
  request({
    url: `/ms/medias`,
    method: "delete",
    params,
  });
// 获取上传视频的授权签名
export const getMediaUpload = (params) =>
  request({
    url: `/ms/medias/signature/upload`,
    method: "get",
    params,
  });
// 上传后的视传到后台
export const mediaUpload = (params) =>
  request({
    url: `/ms/medias`,
    method: "post",
    data: params,
  });
// 修改媒资元数据
export const updateMedia = (id, params) =>
  request({
    url: `/ms/medias/${id}`,
    method: "put",
    data: params,
  });
// 媒资统计
export const getMediaStatistics = () =>
  request({
    url: `/ms/medias/statistics`,
    method: "get",
  });
// 本地文件服务上传
export const uploadFile = (file) => {
  const formData = new FormData();
  formData.append("file", file);
  return request({
    url: `/file/upload`,
    method: "post",
    data: formData,
  });
};
// 管理端获取预览视频的授权签名
export const getMediasSignature = (params) =>
  request({
    url: `/ms/medias/signature/preview`,
    method: "get",
    params,
  });
