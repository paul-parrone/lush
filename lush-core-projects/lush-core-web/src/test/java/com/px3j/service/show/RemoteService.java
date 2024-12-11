package com.px3j.service.show;

import com.px3j.lush.core.model.AnyModel;

public interface RemoteService {
    AnyModel ping(String ticket);
}
