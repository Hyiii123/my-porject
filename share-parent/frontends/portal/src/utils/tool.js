// 转换 A、B、C、D ...
export const upperAlpha = (num, type) => {
  if (num === null || num === undefined) return ''
  const n = Number(num)
  if (isNaN(n)) return String(num)
  const index = (type === 0 || n === 0) ? n : n - 1
  if (index >= 0 && index < 26) {
    return String.fromCharCode(65 + index)
  }
  return String(num)
}
// 时间转换 h:m:s
export const timeFormat = (time) => {
    //  秒
    let second = parseInt(time)
    //  分
    let minute = '00'
    //  小时
    let hour = '00'

    if (second > 60) {
        //  获取分钟，除以60取整数，得到整数分钟
        minute = parseInt(second / 60)
        //  获取秒数，秒数取佘，得到整数秒数
        second = parseInt(second % 60)
        //  如果分钟大于60，将分钟转换成小时
        if (minute > 60) {
          //  获取小时，获取分钟除以60，得到整数小时
          hour = parseInt(minute / 60)
          //  获取小时后取佘的分，获取分钟除以60取佘的分
          minute = parseInt(minute % 60)
        }
      }
    return `${hour} : ${minute} : ${second}`
}

export const amountConversion = (item) => {
  let amount = '0'
  if (item){
    amount = (item / 100).toFixed(2)
  } 
  return amount
}