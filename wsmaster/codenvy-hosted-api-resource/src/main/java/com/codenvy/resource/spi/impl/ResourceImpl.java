/*
 *  [2012] - [2016] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.resource.spi.impl;

import com.codenvy.resource.model.Resource;
import com.google.common.base.Objects;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * @author Sergii Leschenko
 */
@Entity(name = "Resource")
public class ResourceImpl implements Resource {
    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private String type;

    @Basic(optional = false)
    private long amount;

    @Column(nullable = false)
    private String unit;

    public ResourceImpl() {}

    public ResourceImpl(String type, long amount, String unit) {
        this.amount = amount;
        this.type = type;
        this.unit = unit;
    }

    public ResourceImpl(Resource resource) {
        this(resource.getType(),
             resource.getAmount(),
             resource.getUnit());
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public long getAmount() {
        return amount;
    }

    @Override
    public String getUnit() {
        return unit;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResourceImpl)) return false;
        ResourceImpl resource = (ResourceImpl)o;
        return Objects.equal(amount, resource.amount) &&
               Objects.equal(getType(), resource.getType()) &&
               Objects.equal(getUnit(), resource.getUnit());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getType(), amount);
    }

    @Override
    public String toString() {
        return "ResourceImpl{" +
               "type='" + type + '\'' +
               ", amount=" + amount +
               ", unit=" + unit +
               '}';
    }
}
