package com.obar.dal;

import com.obar.config.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;
import java.util.Optional;

public abstract class BaseRepository<T, ID> {

    private final Class<T> entityClass;

    protected BaseRepository(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    protected Session getSession() {
        return HibernateUtil.getSessionFactory().openSession();
    }

    public T save(T entity) {
        Transaction tx = null;
        try (Session session = getSession()) {
            tx = session.beginTransaction();
            session.persist(entity);
            tx.commit();
            return entity;
        } catch (Exception e) {
            if (tx != null)
                tx.rollback();
            throw e;
        }
    }

    public Optional<T> findById(ID id) {
        try (Session session = getSession()) {
            return Optional.ofNullable(session.find(entityClass, id));
        }
    }

    public List<T> findAll() {
        try (Session session = getSession()) {
            String query = "FROM " + entityClass.getSimpleName();
            return session.createQuery(query, entityClass).list();
        }
    }

    public T update(T entity) {
        Transaction tx = null;
        try (Session session = getSession()) {
            tx = session.beginTransaction();
            T merged = session.merge(entity);
            tx.commit();
            return merged;
        } catch (Exception e) {
            if (tx != null)
                tx.rollback();
            throw e;
        }
    }

    public void delete(T entity) {
        Transaction tx = null;
        try (Session session = getSession()) {
            tx = session.beginTransaction();
            session.remove(session.contains(entity) ? entity : session.merge(entity));
            tx.commit();
        } catch (Exception e) {
            if (tx != null)
                tx.rollback();
            throw e;
        }
    }

    public void deleteById(ID id) {
        findById(id).ifPresent(this::delete);
    }
}
