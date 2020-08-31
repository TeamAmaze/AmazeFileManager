/*
 * Copyright (C) 2014-2020 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com> and Contributors.
 *
 * This file is part of Amaze File Manager.
 *
 * Amaze File Manager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*package com.amaze.filemanager.utils.aop;

import android.util.Log;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public class AsyncAspect {

  @Pointcut("within(com.amaze.filemanager..*)")
  public void asyncBlockPointcut() {}

  @Around("asyncBlockPointcut() && @annotation(com.amaze.filemanager.utils.annotations.BlockingAsync)")
  public Object asyncBlockAspect(ProceedingJoinPoint proceedingJoinPoint) {
    try {
      return Observable.just(proceedingJoinPoint.proceed()).subscribeOn(Schedulers.io()).blockingSingle();
    } catch (Throwable throwable) {
      Log.w(getClass().getSimpleName(), "Failed to proceed to join point due to: {}", throwable);
      return null;
    }
  }
}*/
