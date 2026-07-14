# 设计规范

## 颜色

| 名称 | 色值 | 用途 |
|------|------|------|
| PrimaryBlue | `#1976D2` | 主色调（按钮、强调） |
| StickyGold | `#B8860B` | 固定便签标签 |
| NearBlack | `#1A1A1A` | 主要文字 |
| DarkGray | `#666666` | 次要文字 |
| White | `#FFFFFF` | 卡片背景 |
| OffWhite | `#F5F5F5` | 输入框背景 |
| ErrorRed | `#E53935` | 删除/错误 |

## 通知栏样式

```xml
<shape>
    <gradient startColor="#1A2979B3" centerColor="#331A4F8A" endColor="#1A1A4F8A" angle="135"/>
    <corners radius="16dp"/>
    <stroke width="0.5dp" color="#33FFFFFF"/>
</shape>
```

- 标题：`#FFFFFF 18sp Medium`
- 时间：`#99FFFFFF 13sp Regular`
- 分隔符：`#99FFFFFF 14sp`

## UI 规范
- 圆角：卡片 16dp，输入框 8dp，按钮 12dp
- 间距：列表 12dp，内边距 16dp
- 阴影：卡片 `elevation=1dp`，FAB `elevation=8dp`

## 对话框规范
- AddEditDialog：标题 24sp Bold + 输入框 minHeight 120dp
- ReminderDialog：折叠式内容 + 日期时间选择器
- 最大高度 560dp，超出可滚动
