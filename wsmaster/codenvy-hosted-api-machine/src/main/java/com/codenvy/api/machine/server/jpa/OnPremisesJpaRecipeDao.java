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
package com.codenvy.api.machine.server.jpa;

import com.codenvy.api.machine.server.recipe.RecipePermissionsImpl;
import com.google.inject.persist.Transactional;

import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.machine.server.jpa.JpaRecipeDao;
import org.eclipse.che.api.machine.server.recipe.RecipeImpl;
import org.eclipse.che.api.machine.server.spi.RecipeDao;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.List;

/**
 * JPA {@link RecipeDao} implementation that respects permissions on search by user.
 *
 * @author Max Shaposhnik (mshaposhnik@codenvy.com)
 * @author Anton Korneta
 */
@Singleton
public class OnPremisesJpaRecipeDao extends JpaRecipeDao {

    @Inject
    private Provider<EntityManager> managerProvider;

    /**
     * Translated query should look like:
     *
     * SELECT recipe.ID, recipe.CREATOR, recipe.DESCRIPTION, recipe.NAME, recipe.SCRIPT, recipe.TYPE
     * FROM {oj RECIPEPERMISSIONS permission LEFT OUTER JOIN
     *          RECIPE recipe ON (recipe.ID = permission.RECIPEID) LEFT OUTER JOIN
     *          Recipe_TAGS tag ON (tag.Recipe_ID = recipe.ID)},
     *      RECIPEPERMISSIONS_ACTIONS permissionActions
     * WHERE ( (tag.tag IN (?))
     *          AND ((? IS NULL) OR (recipe.TYPE IS NULL) OR (recipe.TYPE = ?))
     *          AND ((permission.USERID IS NULL) OR (permission.USERID = ?))
     *          AND (permissionActions.actions = ?)
     *          AND (permissionActions.RECIPEPERMISSIONS_ID = permission.ID) )
     * GROUP BY recipe.ID HAVING (COUNT(tag.tag) = ?)
     */
    @Override
    @Transactional
    public List<RecipeImpl> search(String userId,
                                   List<String> tags,
                                   String type,
                                   int skipCount,
                                   int maxItems) throws ServerException {
        try {
            final EntityManager em = managerProvider.get();
            final CriteriaBuilder cb = em.getCriteriaBuilder();
            final CriteriaQuery<RecipeImpl> query = cb.createQuery(RecipeImpl.class);
            final Root<RecipePermissionsImpl> perm = query.from(RecipePermissionsImpl.class);
            final Join<RecipeImpl, RecipePermissionsImpl> rwp = perm.join("recipe", JoinType.LEFT);
            final Expression<List<String>> acts = perm.get("actions");
            final ParameterExpression<String> typeParam = cb.parameter(String.class, "recipeType");
            final Predicate checkType = cb.or(cb.isNull(typeParam),
                                              cb.isNull(rwp.get("type")),
                                              cb.equal(rwp.get("type"), typeParam));
            final Predicate userIdCheck = cb.or(cb.isNull(perm.get("userId")),
                                                cb.equal(perm.get("userId"), cb.parameter(String.class, "userId")));
            final Predicate searchActionCheck = cb.isMember(cb.parameter(String.class, "actionParam"), acts);
            final Predicate shareCheck = cb.and(checkType, userIdCheck, searchActionCheck);
            final TypedQuery<RecipeImpl> typedQuery;
            if (tags != null && !tags.isEmpty()) {
                final Join<RecipeImpl, String> tag = rwp.join("tags", JoinType.LEFT);
                query.select(cb.construct(RecipeImpl.class, rwp))
                     .where(cb.and(tag.in(tags), shareCheck))
                     .groupBy(rwp.get("id"))
                     .having(cb.equal(cb.count(tag), tags.size()));
                typedQuery = em.createQuery(query).setParameter("tags", tags);
            } else {
                typedQuery = em.createQuery(query.select(cb.construct(RecipeImpl.class, rwp))
                                                 .where(shareCheck));
            }
            return typedQuery.setParameter("userId", userId)
                             .setParameter("recipeType", type)
                             .setParameter("actionParam", "search")
                             .setFirstResult(skipCount)
                             .setMaxResults(maxItems)
                             .getResultList();
        } catch (RuntimeException ex) {
            throw new ServerException(ex.getLocalizedMessage(), ex);
        }
    }
}
