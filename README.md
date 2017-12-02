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
```
上述的方法称为外部拦截法，处理滑动冲突还有另外一种思路，也称为内部拦截法，其具体思路为令父容器不拦截事件，而直接将事件传递给子 View，当父容器需要此事件时，在子 View 中调用父容器的 requestDisallowInterceptTouchEvent(false) 方法，令父容器去拦截事件。具体的步骤如下：

1. 重写子 View 的 dispatchTouchEvent  方法。
2. 对于 ACTION_DOWN 事件，调用 requestDisallowInterceptTouchEvent(true) 方法，令父容器对于后续事件都不拦截。
3. 对于 ACTION_MOVE 事件，根据具体的要求去处理。若此父控件需要此事件，则调用 requestDisallowInterceptTouchEvent(false) 方法，令父容器拦截事件。
4. 重写父容器的  onInterceptTouchEvent 方法。
5. 对于 ACTION_DOWN 事件，直接返回 false。
6. 对于其它事件，均返回 true。

其代码如下：
```java
    # 子 View
    public boolean dispatchTouchEvent(MotionEvent event) {
           int x = (int) event.getX();
           int y = (int) event.getY();
           switch (event.getAction()) {
               case MotionEvent.ACTION_DOWN:
                   //调用此方法后对于后续事件父容器将直接跳过 onInterceTouchEvent() 方法，直接传给子 View
                   parent.requestDisallowInterceptTouchEvent(true);
                   break;
               case MotionEvent.ACTION_MOVE:
                   int deltaX = x - mLastXIntercept;
                   int deltaY = y - mLastYIntercept;
                   if (父容器需要此事件) {
                   	   //调用后父容器将会调用 onInterceTouchEvent() 方法，而我们又重写了父容器的 onInterceTouchEvent() 方法，让其对除 ACTION_DOWN 之外的事件都返回 true，即让父容器拦截事件
                       parent.requestDisallowInterceptTouchEvent(true);
                   }
                   break;
               case MotionEvent.ACTION_UP:
                   break;
               default:
                   break;
           }
           return super.dispatchTouchEvent(event);
    }
    
    # 父 View
    public boolean onInterceTouchEvent(MotionEvent event) {
           if (event.getAction() == MotionEvent.ACTION_DOWN) {
               return false;
           } else {
               return true;
           }
    }
```    

下面从源码角度分析原因，先来看一下 ViewGroup 的 requestDisallowInterceptTouchEvent 方法：
```java
        @Override
        public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
    
            if (disallowIntercept == ((mGroupFlags & FLAG_DISALLOW_INTERCEPT) != 0)) {
                // We're already in this state, assume our ancestors are too
                return;
            }
    
            if (disallowIntercept) {
                mGroupFlags |= FLAG_DISALLOW_INTERCEPT;
            } else {
                mGroupFlags &= ~FLAG_DISALLOW_INTERCEPT;
            }
    
            // Pass it up to our parent
            if (mParent != null) {
                mParent.requestDisallowInterceptTouchEvent(disallowIntercept);
            }
        }
```
再看一下 ViewGroup 的 dispatchTouchEvent 方法中的一段代码：
```java
    final boolean intercepted;
    if (actionMasked == MotionEvent.ACTION_DOWN
            || mFirstTouchTarget != null) {
         //注意下面这行代码
         final boolean disallowIntercept = (mGroupFlags & FLAG_DISALLOW_INTERCEPT) != 0;
         if (!disallowIntercept) {
             intercepted = onInterceptTouchEvent(ev);
             ev.setAction(action); // restore action in case it was changed
         } else {
             intercepted = false;
         }
    } else {
         // There are no touch targets and this action is not an initial down
         // so this view group continues to intercept touches.
         intercepted = true;
    }
```
结合上面两段代码可以看出，当调用requestDisallowInterceptTouchEvent(true)  时，disallowIntercept 将变为 true（具体为什么是属于细节的东西，就不深究了...），于是将跳过 onInterceptTouchEvent() 方法，即不拦截事件。当调用requestDisallowInterceptTouchEvent(false) 时，disallowIntercept 为 false，onInterceptTouchEvent() 方法会被调用，而我们重写了 ViewGroup 的 onInterceptTouchEvent() 方法，让其对于除 ACTION_DOWN 事件之外都返回 true，于是父容器就拦截了事件。

