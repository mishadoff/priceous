-- :name create-job :! :n
-- :doc Creates job
insert into jobs
  (id, type, status, data, started_ts, finished_ts)
values
  (:id, :type, :status, :data, :started_ts, null);