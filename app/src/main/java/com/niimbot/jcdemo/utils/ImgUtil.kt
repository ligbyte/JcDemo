package com.niimbot.jcdemo.utils;

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.util.Log
import com.niimbot.jcdemo.bean.Dish

private const val TAG = "ImgUtil"
/**
 * @author 张彬
 * @date 2023年12月11日1:36 PM
 */
class ImgUtil {


    companion object {
        /**
         * 获取文本宽度
         * @param paint Paint
         * @param text String
         * @return Int
         */
        fun getFontWidth(paint: Paint, text: String): Int {
            val rect = Rect()
            paint.getTextBounds(text, 0, text.length, rect)
            return rect.width()
        }

        /**
         *判断菜品需要显示几行
         *
         * @param paint Paint
         * @param text String
         * @return Int
         */
        fun getFontHeight(paint: Paint, text: String): Int {
            val rect = Rect()
            paint.getTextBounds(text, 0, 1, rect)
            return rect.height()
        }

        /**
         * 获取打印图片的菜品需要打印的高度
         *
         * @param list 菜品列表
         * @param normalFontHeight 正常字体高度
         * @param perLineDistance 行与行之间的距离
         * @param toKitchen 是否发送到厨房
         * @return 菜品需要打印的高度
         */
        fun getProBmpHeight(
            list: ArrayList<Dish>, normalFontHeight: Int, perLineDistance: Int, toKitchen: Boolean
        ): Int {
            var h = 0 // 总高度
            for (i in list.indices) {
                // 根据菜品文本长度显示多行
                var lines = getShowLine(list[i].name)
                for (j in 0 until lines) {
                    h += normalFontHeight + perLineDistance
                }

                //如果是发送到厨房的需要计算备注的高度
                if (toKitchen) {
                    //显示备注
                    lines = getShowLine(list[i].note)
                    for (j in 0 until lines) {
                        h += normalFontHeight + perLineDistance
                    }
                }
                h += perLineDistance // 菜品之间距离大点，方便区分
            }
            return h
        }

        /**
         * 计算菜品需要显示的行数
         *
         * @param dish 菜品名称
         * @return 菜品需要显示的行数
         */
        fun getShowLine(dish: String): Int {
            var line = 1 // 默认显示一行

            // 如果菜品名称长度超过12个字符，需要计算显示的行数
            if (dish.length > 12) {
                line = if (dish.length % 12 == 0) {
                    dish.length / 12 // 整除情况下的行数
                } else {
                    dish.length / 12 + 1 // 有余数时的行数
                }
            }

            return line
        }


        /**
         * 截取每行需要显示的菜品文本
         *
         * @param dishName 菜品名称
         * @param lineIndex 行索引
         * @param lines 总行数
         * @return 每行需要显示的菜品文本
         */
        fun getDisplayTextForLine(dishName: String, lineIndex: Int, lines: Int): String {
            val subStr: String = if (lines == 1) {
                dishName
            } else if (lineIndex != lines - 1 || dishName.length % 12 == 0) {
                dishName.substring(lineIndex * 12, (lineIndex + 1) * 12)
            } else {
                dishName.substring(lineIndex * 12)
            }
            return subStr
        }

        /**
         * 把pos端小票信息转成图片
         *
         * @return
         */
        fun generatePosReceiptImage(pros: ArrayList<Dish>): Bitmap {
            val paint = Paint().apply {
                textSize = 48f
                color = Color.BLACK
                style = Paint.Style.FILL
                typeface = Typeface.DEFAULT_BOLD
                isAntiAlias = false
                isDither = false
                hinting = Paint.ANTI_ALIAS_FLAG
            }

            val restaurantInfo = arrayOf(
                "盛图食堂",
                "电话：16639439562",
                "日期: 2020/03/25   15:50",
                "区域: 大厅",
                "桌位: A04",
                "上座人数: 5人",
                "服务员: Anny",
                "订单号: 4884977449494949494"
            )

            val restaurantNameHeight = getFontHeight(paint, "盛图食堂")
            val blackFontHeight = getFontHeight(paint.apply { textSize = 32f }, "盛图食堂")
            val normalFontHeight =
                getFontHeight(paint.apply { typeface = Typeface.DEFAULT }, "盛图食堂")

            Log.d(TAG, "blackFontHeight:${blackFontHeight} ,normalFontHeight:${normalFontHeight} ")

            val perLineDistance = 28
            val perFontDistance = 28
            //小票宽度
            val receiptCanvasWidth = 432
            val topBlank = 64
            //订单信息与店铺名称之间的间距
            val restaurantOrderSpacing = 100
            //店铺名高度+标准字号高度+行间距
            val receiptCanvasHeadHeight =topBlank+ restaurantNameHeight+normalFontHeight+perLineDistance+restaurantOrderSpacing+(restaurantInfo.size-3)*(normalFontHeight+perLineDistance)

            //底部留白（可能要撕纸，需要留白）
            val bottomBlank = 200
            val welcomeMsgDistance = 100
            //小票菜单标题栏高度+总计金额
            val receiptCanvasBottomHeight =perLineDistance*7+blackFontHeight*3+ perFontDistance*2+welcomeMsgDistance+normalFontHeight*2+ bottomBlank
            //小票高度
            val receiptCanvasHeight =
                receiptCanvasHeadHeight + receiptCanvasBottomHeight + getProBmpHeight(pros, normalFontHeight, perLineDistance, false)



            //创建小票图片
            val bmp = Bitmap.createBitmap(
                receiptCanvasWidth,
                receiptCanvasHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bmp)
            canvas.drawColor(Color.WHITE)

            var textWidth: Int

            var y: Int = restaurantNameHeight+topBlank
            var x: Float
            restaurantInfo.forEachIndexed { index, str ->
                when (index) {
                    0 -> {
                        paint.apply {
                            textSize = 48f
                            typeface = Typeface.DEFAULT_BOLD
                        }


                    }

                    1 -> {
                        paint.apply {
                            textSize = 28f
                            typeface = Typeface.DEFAULT
                        }
                        y += normalFontHeight + perLineDistance
                    }

                    2 -> {
                        paint.apply {
                            textSize = 28f
                        }
                        y += restaurantOrderSpacing
                    }

                    3,4, 5,6, 7 -> {
                        y += blackFontHeight + perLineDistance
                    }

                    else -> {
                        paint.apply {
                            typeface = Typeface.DEFAULT
                        }
                    }
                }

                textWidth = getFontWidth(paint, str)
                x = when (index) {
                    0, 1 -> {
                        (receiptCanvasWidth / 2 - textWidth / 2).toFloat()
                    }

                    2, 3, 4,5,6, 7 -> {
                        16f
                    }

                    else -> {
                        receiptCanvasWidth - textWidth - 50f - 16f
                    }
                }
                canvas.drawText(
                    str,
                    x,
                    y.toFloat(),
                    paint
                )

            }

            paint.apply {
                typeface = Typeface.DEFAULT_BOLD
                strokeWidth = 4f
            }
            //画线
            y += perLineDistance
            canvas.drawLine(0f, y.toFloat(), receiptCanvasWidth.toFloat(), y.toFloat(), paint)


            //title(菜品、单价、数量、小计)
            y += perLineDistance * 2
            canvas.drawText("菜品", 16f, y.toFloat(), paint)
            canvas.drawText(
                "单价",
                (receiptCanvasWidth - getFontWidth(paint, "小计") - 50 - 100 - 100).toFloat(),
                y.toFloat(),
                paint
            )
            canvas.drawText(
                "数量",
                (receiptCanvasWidth - getFontWidth(paint, "小计") - 50 - 100).toFloat(),
                y.toFloat(),
                paint
            )
            canvas.drawText(
                "小计",
                (receiptCanvasWidth - getFontWidth(paint, "小计") - 50).toFloat(),
                y.toFloat(),
                paint
            )
            //画线
            y += blackFontHeight
            canvas.drawLine(0f, y.toFloat(), receiptCanvasWidth.toFloat(), y.toFloat(), paint)
            paint.apply {
                typeface = Typeface.DEFAULT
            }
            y += perLineDistance * 2
            //下面是画菜品文本
            for (i in 0 until pros.size) {
                //先画后面的值，菜品名称后面画，因为菜品名称可能多行显示，以菜品显示高度为准；
                canvas.drawText(
                    "￥" + pros[i].price,
                    (receiptCanvasWidth - getFontWidth(paint, "小计") - 50 - 100 - 100).toFloat(),
                    y.toFloat(),
                    paint
                )
                canvas.drawText(
                    java.lang.String.valueOf(pros[i].count),
                    (receiptCanvasWidth - getFontWidth(paint, "小计") - 50 - 100).toFloat(),
                    y.toFloat(),
                    paint
                )
                canvas.drawText(
                    "￥ ${pros[i].price * pros[i].count}",
                    (receiptCanvasWidth - getFontWidth(paint, "小计") - 50).toFloat(),
                    y.toFloat(),
                    paint
                )
                var dish: String
                //根据菜品文本长度显示多行
                val lines: Int = getShowLine(pros[i].name)
                for (j in 0 until lines) {
                    dish = getDisplayTextForLine(pros[i].name, j, lines)
                    canvas.drawText(dish, 16f, y.toFloat(), paint)
                    y += normalFontHeight + perLineDistance
                }
                y += perLineDistance
            }
            //画线
            canvas.drawLine(0f, y.toFloat(), receiptCanvasWidth.toFloat(), y.toFloat(), paint)


            //下面是各种金额
            y += perLineDistance * 2
            paint.apply {
                typeface = Typeface.DEFAULT_BOLD
            }

            canvas.drawText("店铺实收", 16f, y.toFloat(), paint)
            canvas.drawText(
                "￥1000",
                (receiptCanvasWidth - getFontWidth(paint, "￥1000") - 50).toFloat(),
                y.toFloat(),
                paint
            )

            y += blackFontHeight + perLineDistance
            canvas.drawText("消费税", 16f, y.toFloat(), paint)
            canvas.drawText(
                "￥50",
                (receiptCanvasWidth - getFontWidth(paint, "￥50") - 50).toFloat(),
                y.toFloat(),
                paint
            )

            y += blackFontHeight + perFontDistance
            canvas.drawText("服务费", 16f, y.toFloat(), paint)
            canvas.drawText(
                "￥100",
                (receiptCanvasWidth - getFontWidth(paint, "￥100") - 50).toFloat(),
                y.toFloat(),
                paint
            )

            y += blackFontHeight + perFontDistance
            canvas.drawText("折扣金额", 16f, y.toFloat(), paint)
            canvas.drawText(
                "-￥100",
                (receiptCanvasWidth - getFontWidth(paint, "-￥100") - 50).toFloat(),
                y.toFloat(),
                paint
            )


            //画线
            y += blackFontHeight + perFontDistance
            canvas.drawLine(0f, y.toFloat(), receiptCanvasWidth.toFloat(), y.toFloat(), paint)

            paint.apply {
                textSize = 25f
                typeface = Typeface.DEFAULT_BOLD
            }
            y += blackFontHeight + perFontDistance
            canvas.drawText("最终实收", 16f, y.toFloat(), paint)
            canvas.drawText(
                "￥20000",
                (receiptCanvasWidth - getFontWidth(paint, "￥20000") - 50).toFloat(),
                y.toFloat(),
                paint
            )


            paint.apply {
                textSize = 25f
                typeface = Typeface.DEFAULT_BOLD
            }
            y += welcomeMsgDistance
            var welcomeMsg = "谢谢光临,欢迎下次再来"
            textWidth = getFontWidth(paint, welcomeMsg)
            canvas.drawText(
                welcomeMsg,
                (receiptCanvasWidth / 2 - textWidth / 2).toFloat(),
                y.toFloat(),
                paint
            )

            y += normalFontHeight
            welcomeMsg = "@洛阳盛图科技有限公司提供技术支持"
            textWidth = getFontWidth(paint, welcomeMsg)
            canvas.drawText(
                welcomeMsg,
                (receiptCanvasWidth / 2 - textWidth / 2).toFloat(),
                y.toFloat(),
                paint
            )

            y += bottomBlank

            return bmp
        }

    }
}