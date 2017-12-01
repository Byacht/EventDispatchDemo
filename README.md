# 事件分发机制
当触摸屏幕时，会调用 ViewGroup 的 dispatchTouchEvent 方法和 onInterceptTouchEvent 方法，由此来判断是否拦截此 event 事件。onInterceptTouchEvent 方法返回 true，代表拦截此事件，此事件将交由该 ViewGroup 的 onTouchEvent 方法处理；若返回 false，则调用子 View 的 dispatchTouchEvent 方法，将该事件传递到下一个 View 中，继续下一轮分发事件。上述过程的伪代码如下：

    public boolean dispatchTouchEvent(MotionEvent ev) {
        boolean consume = false;
        if(onInterceptTouchEvent(ev)) {
            comsume = onTounchEvent(ev);
        } else {
            consume = child.dispatchTouchEvent(ev);
        }
      	return consume;
    }

- ViewGroup 的onInterceptTouchEvent 默认返回false，即不拦截事件
- 若ViewGroup 的onInterceptTouchEvent 方法在 MotionEvent.Action_down 方法中返回true，则后续的事件（如MOVE、UP 都将交由该ViewGroup 处理，不会再传递到子 View 中）
- View 没有 onInterceptTouchEvent 方法，若事件传递到 View，则直接调用其 onTouchEvent方法
- 若 View 的 onTouchEvent 方法返回 true，则其父 View 的 onTouchEvent 不会被调用；若返回 false，则还会继续调用到父 View的 onTouchEvent 方法。View 的 onTouchEvent方法默认返回 true，若它是不可点击状态（即它的 clickable 属性和 longclickable 属性都为 false），则返回 false。

因此，当需要处理滑动冲突时，可通过如下思路解决：

1. 重写 ViewGroup 的 onInterceptTouchEvent 方法。
2. 对于 ACTION_DOWN 事件返回 false。（原因参考上述第二条）
3. 对于 ACTION_MOVE 事件，根据具体的要求去处理。若此父控件需要此事件，则返回 true；否则返回 false。
4. 对于 ACTION_UP 事件，一般默认返回 false。因为若返回 true，子 View 将无法接收到此 ACTION_UP 事件，造成其无法监听点击事件（子 View 的点击事件监听触发条件为接收到 ACTION_DOWN 和 ACTION_UP。

上述过程的代码如下：

```java
    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        boolean intercepted = false;
        int x = (int) event.getX();
        int y = (int) event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                intercepted = false;
                break;
            case MotionEvent.ACTION_MOVE:
                int deltaX = x - mLastXIntercept;
                int deltaY = y - mLastYIntercept;
                if (父类需要此事件) {
                    intercepted = true;
                } else {
                    intercepted = false;
                }
                break;
            case MotionEvent.ACTION_UP:
                intercepted = false;
                break;
            default:
                break;
        }

        mLastXIntercept = x;
        mLastYIntercept = y;
        return intercepted;
    }
