# electrocardiogram

## 使用方法

首先初始化

``` java
//实例化控件
mElectrocardiogram = (ElectrocardiogramView) findViewById(R.id.electrocardiogram);
//设置每行点的个数
mElectrocardiogram.setMaxPointAmount(300);
//当绘制满后从头开始绘制时，擦出后面点的个数
mElectrocardiogram.setRemovedPointNum(10);
//设置背景网格为每10个点画一条细线，每50个点画一条粗线
mElectrocardiogram.setEveryNPoint(10,50);
```

然后引用

``` java
mElectrocardiogram.setLinePoint(msg.arg1, msg.arg2);
```
