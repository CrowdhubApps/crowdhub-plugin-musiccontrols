package com.homerours.musiccontrols

import android.app.Service
import android.os.Binder

class KillBinder(val service: Service) : Binder()