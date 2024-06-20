package com.mysite.core.GenericPressCoverageCF.service;

import com.adobe.cq.dam.cfm.ContentFragmentException;
import com.day.cq.replication.ReplicationException;

public interface ContentFragmentService {
    void  createContentFragment() throws ContentFragmentException, ReplicationException;
}
